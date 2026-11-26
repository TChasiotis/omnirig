package com.omnirig.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MiniJson {

    private final String src;
    private int pos = 0;

    private MiniJson(String src) {
        this.src = src;
    }

    public static Map<String, Object> parseObject(String json) {
        MiniJson p = new MiniJson(json);
        Object res = p.readValue();
        if (res instanceof Map<?, ?> m) {
            Map<String, Object> out = new LinkedHashMap<>();
            m.forEach((k, v) -> out.put(String.valueOf(k), v));
            return out;
        }
        throw new IllegalArgumentException("Μη έγκυρο JSON object.");
    }

    private Object readValue() {
        skipWs();
        if (pos >= src.length()) return null;
        char c = src.charAt(pos);
        if (c == '{') return readObject();
        if (c == '[') return readArray();
        if (c == '"') return readString();
        if (c == 't' && src.startsWith("true", pos))  { pos += 4; return Boolean.TRUE; }
        if (c == 'f' && src.startsWith("false", pos)) { pos += 5; return Boolean.FALSE; }
        if (c == 'n' && src.startsWith("null", pos))  { pos += 4; return null; }
        return readNumber();
    }

    private Map<String, Object> readObject() {
        Map<String, Object> map = new LinkedHashMap<>();
        pos++;
        skipWs();
        if (pos < src.length() && src.charAt(pos) == '}') { pos++; return map; }
        while (pos < src.length()) {
            skipWs();
            String key = readString();
            skipWs();
            if (pos < src.length() && src.charAt(pos) == ':') pos++;
            Object val = readValue();
            map.put(key, val);
            skipWs();
            if (pos < src.length() && src.charAt(pos) == ',') { pos++; continue; }
            if (pos < src.length() && src.charAt(pos) == '}') { pos++; break; }
        }
        return map;
    }

    private List<Object> readArray() {
        List<Object> list = new ArrayList<>();
        pos++;
        skipWs();
        if (pos < src.length() && src.charAt(pos) == ']') { pos++; return list; }
        while (pos < src.length()) {
            list.add(readValue());
            skipWs();
            if (pos < src.length() && src.charAt(pos) == ',') { pos++; continue; }
            if (pos < src.length() && src.charAt(pos) == ']') { pos++; break; }
        }
        return list;
    }

    private String readString() {
        pos++;
        StringBuilder sb = new StringBuilder();
        while (pos < src.length()) {
            char c = src.charAt(pos++);
            if (c == '"') break;
            if (c == '\\' && pos < src.length()) {
                char esc = src.charAt(pos++);
                switch (esc) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case '/' -> sb.append('/');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        if (pos + 4 <= src.length()) {
                            sb.append((char) Integer.parseInt(src.substring(pos, pos + 4), 16));
                            pos += 4;
                        }
                    }
                    default -> sb.append(esc);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private Number readNumber() {
        int start = pos;
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') pos++;
            else break;
        }
        String num = src.substring(start, pos);
        if (num.contains(".") || num.contains("e") || num.contains("E")) {
            return Double.parseDouble(num);
        }
        long l = Long.parseLong(num);
        return (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) ? (int) l : l;
    }

    private void skipWs() {
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) pos++;
    }
}