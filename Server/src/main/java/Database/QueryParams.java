package Database;

import java.sql.Timestamp;

public class QueryParams {
    private Timestamp start;
    private Timestamp end;
    private String filter;
    private String operator;
    private String value;
    private String sortBy;
    private String order;
    private int limit = 200;
    private int offset = 0;

    public Timestamp getStart() { return start; }
    public void setStart(Timestamp start) { this.start = start; }

    public Timestamp getEnd() { return end; }
    public void setEnd(Timestamp end) { this.end = end; }

    public String getFilter() { return filter; }
    public void setFilter(String filter) { this.filter = filter; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }

    public String getOrder() { return order; }
    public void setOrder(String order) { this.order = order; }

    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }

    public int getOffset() { return offset; }
    public void setOffset(int offset) { this.offset = offset; }
}
