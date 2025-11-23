package core;

public final class Tag {
    private final String name; // e.g. "A1", "M2", "L3", "S1"

    public Tag(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Tag)) return false;
        return name.equals(((Tag) o).name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }

    public static final Tag NONE = new Tag("0"); // corresponds to Qi = 0 in slides
}
