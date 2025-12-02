package com.example.dbmigrator.migration;

public record Migration(int version, String description, String scriptName, String checksum,
                        String sql) implements Comparable<Migration> {

    @Override
    public int compareTo(Migration o) {
        return Integer.compare(this.version, o.version);
    }
}