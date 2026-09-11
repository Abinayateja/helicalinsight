package com.helical.mongodb;

import org.bson.Document;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for parsing SQL queries into MongoDB operations.
 */
public class MongoSqlParser {

    public static class ParsedQuery {
        public boolean isSelectOne;
        public boolean isCount;
        public String collection;
        public List<String> projectedColumns = new ArrayList<>();
        public Document filter = new Document();
        public int limit = 0;
        public Document sort = new Document();
    }

    private static final Pattern SELECT_ONE_PATTERN = Pattern.compile("(?i)^SELECT\\s+1(\\s+AS\\s+\\w+)?$");
    private static final Pattern COUNT_PATTERN = Pattern.compile("(?i)^SELECT\\s+COUNT\\s*\\([^)]*\\)\\s+(?:AS\\s+\\w+\\s+)?FROM\\s+[`\"'\\[]?([a-zA-Z0-9_.-]+)[`\"'\\]]?(?:\\s+WHERE\\s+(.*))?", Pattern.DOTALL);
    private static final Pattern SELECT_PATTERN = Pattern.compile("(?i)^SELECT\\s+(.+?)\\s+FROM\\s+[`\"'\\[]?([a-zA-Z0-9_.-]+)[`\"'\\]]?(?:\\s+WHERE\\s+(.+?))?(?:\\s+ORDER\\s+BY\\s+(.+?))?(?:\\s+LIMIT\\s+(\\d+))?\\s*$", Pattern.DOTALL);

    public static ParsedQuery parse(String sql) {
        if (sql == null) {
            throw new IllegalArgumentException("SQL query cannot be null");
        }
        String cleanSql = sql.trim();
        if (cleanSql.endsWith(";")) {
            cleanSql = cleanSql.substring(0, cleanSql.length() - 1).trim();
        }

        ParsedQuery query = new ParsedQuery();

        if (SELECT_ONE_PATTERN.matcher(cleanSql).matches()) {
            query.isSelectOne = true;
            return query;
        }

        Matcher countMatcher = COUNT_PATTERN.matcher(cleanSql);
        if (countMatcher.find()) {
            query.isCount = true;
            query.collection = cleanCollectionName(countMatcher.group(1));
            return query;
        }

        Matcher selectMatcher = SELECT_PATTERN.matcher(cleanSql);
        if (selectMatcher.find()) {
            String colsPart = selectMatcher.group(1).trim();
            query.collection = cleanCollectionName(selectMatcher.group(2));
            String wherePart = selectMatcher.group(3);
            String orderByPart = selectMatcher.group(4);
            String limitPart = selectMatcher.group(5);

            if ("*".equals(colsPart)) {
                // all columns
            } else {
                String[] cols = colsPart.split(",");
                for (String col : cols) {
                    String cleanCol = col.trim();
                    if (cleanCol.toUpperCase().contains(" AS ")) {
                        String[] aliasSplit = cleanCol.split("(?i)\\s+AS\\s+");
                        cleanCol = aliasSplit[0].trim();
                    }
                    cleanCol = cleanCol.replaceAll("^[`\"'\\[]+|[`\"'\\]]+$", "");
                    query.projectedColumns.add(cleanCol);
                }
            }

            if (wherePart != null && !wherePart.trim().isEmpty()) {
                parseWhereClause(wherePart.trim(), query.filter);
            }

            if (orderByPart != null && !orderByPart.trim().isEmpty()) {
                parseOrderByClause(orderByPart.trim(), query.sort);
            }

            if (limitPart != null && !limitPart.trim().isEmpty()) {
                try {
                    query.limit = Integer.parseInt(limitPart.trim());
                } catch (NumberFormatException ignored) {
                }
            }

            return query;
        }

        query.collection = extractTableNameFallback(cleanSql);
        return query;
    }

    private static String cleanCollectionName(String raw) {
        if (raw == null) return "";
        String cleaned = raw.trim().replaceAll("^[`\"'\\[]+|[`\"'\\]]+$", "");
        if (cleaned.contains(".")) {
            cleaned = cleaned.substring(cleaned.lastIndexOf('.') + 1);
        }
        return cleaned;
    }

    private static void parseWhereClause(String whereClause, Document filter) {
        String[] conditions = whereClause.split("(?i)\\s+AND\\s+");
        for (String cond : conditions) {
            String[] parts = cond.split("=");
            if (parts.length == 2) {
                String key = parts[0].trim().replaceAll("^[`\"'\\[]+|[`\"'\\]]+$", "");
                String rawVal = parts[1].trim();
                Object val;
                if ((rawVal.startsWith("'") && rawVal.endsWith("'")) || (rawVal.startsWith("\"") && rawVal.endsWith("\""))) {
                    val = rawVal.substring(1, rawVal.length() - 1);
                } else if ("true".equalsIgnoreCase(rawVal)) {
                    val = true;
                } else if ("false".equalsIgnoreCase(rawVal)) {
                    val = false;
                } else {
                    try {
                        val = Long.parseLong(rawVal);
                    } catch (NumberFormatException e1) {
                        try {
                            val = Double.parseDouble(rawVal);
                        } catch (NumberFormatException e2) {
                            val = rawVal;
                        }
                    }
                }
                filter.append(key, val);
            }
        }
    }

    private static void parseOrderByClause(String orderByClause, Document sort) {
        String[] parts = orderByClause.split(",");
        for (String part : parts) {
            String item = part.trim();
            int direction = 1;
            if (item.toUpperCase().endsWith(" DESC")) {
                direction = -1;
                item = item.substring(0, item.length() - 5).trim();
            } else if (item.toUpperCase().endsWith(" ASC")) {
                direction = 1;
                item = item.substring(0, item.length() - 4).trim();
            }
            item = item.replaceAll("^[`\"'\\[]+|[`\"'\\]]+$", "");
            sort.append(item, direction);
        }
    }

    private static String extractTableNameFallback(String sql) {
        Pattern p = Pattern.compile("(?i)FROM\\s+[`\"'\\[]?([a-zA-Z0-9_.-]+)[`\"'\\]]?");
        Matcher m = p.matcher(sql);
        if (m.find()) {
            return cleanCollectionName(m.group(1));
        }
        return "";
    }
}
