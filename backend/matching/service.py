"""Local synthetic-route matcher. Provisional thresholds; not road-network map matching."""
from dataclasses import dataclass
from math import radians, sin, cos, atan2, sqrt, ceil
from backend.models import GPSPoint

@dataclass(frozen=True)
class MatchConfig:
    distance_m: float = 50.0
    direction_deg: float = 35.0
    min_overlap_m: float = 100.0
    sample_step_m: float = 10.0
    max_gap_ms: int = 15000
    max_accuracy_m: float = 30.0
    max_speed_mps: float = 60.0
    max_samples: int = 3000

DEFAULT = MatchConfig()

def distance(a,b):
    lat1,lat2=radians(a["lat"]),radians(b["lat"])
    dlat,dlon=lat2-lat1,radians(b["lon"]-a["lon"])
    h=sin(dlat/2)**2+cos(lat1)*cos(lat2)*sin(dlon/2)**2
    return 6371000*2*atan2(sqrt(h),sqrt(max(0,1-h)))

def bearing(a,b):
    x,y=radians(a["lat"]),radians(b["lat"])
    d=radians(b["lon"]-a["lon"])
    return atan2(sin(d)*cos(y),cos(x)*sin(y)-sin(x)*cos(y)*cos(d))*180/3.141592653589793

def samples(points,cfg):
    points=[GPSPoint(**p).model_dump() for p in points]
    if any(b["seq"]<=a["seq"] or b["elapsed_ms"]<=a["elapsed_ms"] for a,b in zip(points,points[1:])):
        raise ValueError("route_order_invalid")
    result=[];track=0
    for a,b in zip(points,points[1:]):
        dt=b["elapsed_ms"]-a["elapsed_ms"];length=distance(a,b)
        good=(b["seq"]==a["seq"]+1 and dt<=cfg.max_gap_ms and length>=5
              and length/(dt/1000)<=cfg.max_speed_mps
              and all(p["accuracy_m"] is not None and p["accuracy_m"]<=cfg.max_accuracy_m for p in [a,b]))
        if not good:
            track+=1;continue
        count=ceil(length/cfg.sample_step_m)
        if len(result)+count>cfg.max_samples:raise ValueError("route_window_too_large")
        heading=bearing(a,b)
        for i in range(count):
            f=(i+.5)/count
            result.append(dict(lat=a["lat"]+(b["lat"]-a["lat"])*f,
                               lon=a["lon"]+(b["lon"]-a["lon"])*f,
                               heading=heading,track=track,weight=length/count,seq=a["seq"]))
    return result

def compare_routes(current_trip_id,historical_trip_id,current_points,historical_points,config=DEFAULT):
    if current_trip_id==historical_trip_id:
        return dict(matched=False,status="excluded",reason="same_trip",overlap_m=0)
    current,history=samples(current_points,config),samples(historical_points,config)
    if not current or not history:
        return dict(matched=False,status="insufficient_data",reason="no_valid_movement",overlap_m=0)
    if current[-1]["seq"] != current_points[-2]["seq"]:
        return dict(matched=False,status="insufficient_data",reason="latest_segment_invalid",overlap_m=0)
    previous=[0.0]*(len(history)+1);best=0.0;best_pair=None
    for i,a in enumerate(current):
        row=[0.0]*(len(history)+1)
        for j,b in enumerate(history):
            angle=abs((a["heading"]-b["heading"]+180)%360-180)
            if angle<=config.direction_deg and distance(a,b)<=config.distance_m:
                contiguous=(i>0 and j>0 and a["track"]==current[i-1]["track"] and b["track"]==history[j-1]["track"])
                row[j+1]=(previous[j] if contiguous else 0)+min(a["weight"],b["weight"])
                if i==len(current)-1 and row[j+1]>best:best=row[j+1];best_pair=(a["seq"],b["seq"])
        previous=row
    return dict(matched=best>=config.min_overlap_m,status="evaluated",overlap_m=round(best,2),
                last_current_seq=best_pair[0] if best_pair else None,
                last_historical_seq=best_pair[1] if best_pair else None,
                thresholds=dict(distance_m=config.distance_m,direction_deg=config.direction_deg,min_overlap_m=config.min_overlap_m),
                algorithm="resampled_contiguous_v1",calibration="synthetic_only")
