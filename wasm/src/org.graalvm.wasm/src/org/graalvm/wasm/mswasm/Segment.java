public class Segment {  
    private Handle handle;  
    private byte type;

    public Segment(Handle handle) {
        this.handle = handle;
        type = SegmentType.SEGTYPE_DATA;
    }

    @Override
    public String toString() {
        return "Segment: (" + handle + ", " + type == SegmentType.SEGTYPE_DATA ? "DATA" : "HANDLE" 
                + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if ( ! (obj instanceof Segment)) {
            return false;
        }

        Segment seg = (Segment)obj;
        return seg.getHandle().equals(handle);
    }

    public Handle getHandle() {
        return handle;
    }

    public byte getType() {
        return type;
    }

    public void setTypeData() {
        this.type = SegmentType.SEGTYPE_DATA;
    }

    public void setTypeHandle() {
        this.type = SegmentType.SEGTYPE_HANDLE;
    }

}