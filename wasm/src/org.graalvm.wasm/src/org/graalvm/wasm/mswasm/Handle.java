
public class Handle {
    private int offset;
    private int base;
    private int bound;
    private boolean isCorrupted;

    public Handle(int base, int offset, int bound, boolean isCorrupted) {
        this.base = base;
        this.offset = offset;
        this.bound = bound;
        this.isCorrupted = isCorrupted;
    }

    @Override
    public String toString() {
        return "Handle: (" + base + ", " + offset + ", " + bound + ", " + isCorrupted + ")"; 
    }

    @Override
    public boolean equals(Object obj) {
        if ( ! (obj instanceof Handle)) {
            return false;
        }

        Handle handle = (Handle) obj;
        return handle.getOffset() == offset && handle.getBase() == base && 
               handle.getBound() == bound;
    }

    public int getOffset() {
        return offset;
    }

    public int getBase() {
        return base;
    }

    public int getBound() {
        return bound;
    }

    public boolean isCorrupted() {
        return isCorrupted;
    }

    public boolean isValid() {
        return ! isCorrupted && base + offset <= bound && base + offset >= base;
    }
}