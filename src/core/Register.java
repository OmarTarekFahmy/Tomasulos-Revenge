package core;

public class Register {
    private double value;
    private Tag qi = Tag.NONE; // Tomasulo: which RS/LoadBuffer will produce it

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }

    public int getIntValue() { return (int) value; } // if you need integer addressing

    public Tag getQi() { return qi; }
    public void setQi(Tag qi) { this.qi = qi; }
}
