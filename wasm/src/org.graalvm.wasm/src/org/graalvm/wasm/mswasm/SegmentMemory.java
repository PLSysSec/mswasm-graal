import java.util.Map;
import java.util.HashMap;

public class SegmentMemory {
    private Map<Segment,Object> segments;

    public SegmentMemory() {
        segments = new HashMap<>();
    }

    public boolean storeToSegment(Segment segment, Object value) {
        if ( ! segmnet.isValid() || ! segments.containsKey(segment)) {
            return false;
        }

        if (value instanceof Handle) {
            
        }
    }
}