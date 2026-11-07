import unittest
from backend.matching.service import compare_routes

def route(reverse=False):
    coords=list(range(7))
    if reverse:coords.reverse()
    return [dict(seq=i,elapsed_ms=i*10000,lat=37.0,lon=127+j*.001,accuracy_m=5) for i,j in enumerate(coords)]

class MatchingTests(unittest.TestCase):
    def compare(self,current,history=None):
        return compare_routes("now","past",current,history if history is not None else route())

    def test_forward_reverse_and_short(self):
        self.assertTrue(self.compare(route())["matched"])
        self.assertFalse(self.compare(route(True))["matched"])
        self.assertFalse(self.compare(route()[:1])["matched"])
        self.assertFalse(self.compare(route()[:2])["matched"])

    def test_stationary_far_and_accuracy(self):
        self.assertFalse(self.compare([dict(p,lon=127) for p in route()])["matched"])
        self.assertFalse(self.compare([dict(p,lat=38) for p in route()])["matched"])
        self.assertFalse(self.compare([dict(p,accuracy_m=100) for p in route()])["matched"])
        self.assertFalse(self.compare([dict(p,accuracy_m=None) for p in route()])["matched"])

    def test_no_bridging_gaps(self):
        self.assertFalse(self.compare([dict(p,elapsed_ms=p["seq"]*20000) for p in route()])["matched"])
        self.assertFalse(self.compare([dict(p,seq=p["seq"]*2) for p in route()])["matched"])

    def test_same_trip_and_invalid_order(self):
        self.assertFalse(compare_routes("same","same",route(),route())["matched"])
        with self.assertRaises(ValueError):self.compare(list(reversed(route())))

    def test_departed_route_does_not_trigger_old_overlap(self):
        points=route()
        points.append(dict(seq=7,elapsed_ms=70000,lat=37.001,lon=127.006,accuracy_m=5))
        self.assertFalse(self.compare(points)["matched"])
