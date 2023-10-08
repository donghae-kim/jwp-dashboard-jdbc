package org.springframework.jdbc.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcTemplate {

    private static final Logger log = LoggerFactory.getLogger(JdbcTemplate.class);

    private final DataSource dataSource;

    public JdbcTemplate(final DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void update(final String sql, final Object... objects) {
        executePreparedStatement(sql, PreparedStatement::executeUpdate, objects);
    }

    public <T> Optional<T> queryForObject(final String sql, final RowMapper<T> rowMapper, final Object... objects) {
        return executePreparedStatement(sql, preparedStatement -> {
            final ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                return Optional.of(rowMapper.execute(rs));
            }
            return Optional.empty();
        }, objects);
    }

    public <T> List<T> queryForObjects(final String sql, final RowMapper<T> rowMapper, final Object... objects) {
        return executePreparedStatement(sql, preparedStatement -> {
            final ResultSet rs = preparedStatement.executeQuery();
            final List<T> results = new ArrayList<>();
            while (rs.next()) {
                results.add(rowMapper.execute(rs));
            }
            return results;
        }, objects);
    }

    private <T> T executePreparedStatement(final String sql, PreparedStatementExecutor<T> preparedStatementExecutor,
                                           final Object... objects) {
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try (final PreparedStatement pstmt = conn.prepareStatement(sql)) {

            log.debug("query : {}", sql);
            for (int i = 0; i < objects.length; i++) {
                pstmt.setObject(i + 1, objects[i]);
            }

            return preparedStatementExecutor.execute(pstmt);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            throw new DataAccessException(e);
        }
    }

    public DataSource getDataSource() {
        return this.dataSource;
    }
}
