package com.batch.process.dataservice.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface RowMapper<T> {

    public T mapRow(ResultSet rs) throws SQLException;

}
