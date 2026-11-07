"""B 담당: 경로 매칭 계약 틀. 실제 알고리즘은 미구현."""
def compare_routes(current_trip_id, historical_trip_id, current_points, historical_points):
    if current_trip_id == historical_trip_id:
        return dict(matched=False, status="excluded", reason="same_trip")
    if min(len(current_points), len(historical_points)) < 2:
        return dict(matched=False, status="insufficient_data")
    return dict(matched=None, status="not_implemented",
                reason="direction_continuity_overlap_pending")
