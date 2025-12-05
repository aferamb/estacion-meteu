package Database;

public class Range {
    private Double min;
    private Double max;

    public Range(Double min, Double max) {
        this.min = min;
        this.max = max;
    }

    public Double getMin() {
        return min;
    }

    public Double getMax() {
        return max;
    }
}
