/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.timestamp;

import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.PostgreSQL82Dialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.test.util.jdbc.TimeZoneConnectionProvider;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect( value = PostgreSQL82Dialect.class)
public class JdbcTimestampWithoutUTCTimeZoneTest
		extends BaseNonConfigCoreFunctionalTestCase {

	private TimeZoneConnectionProvider connectionProvider = new TimeZoneConnectionProvider( "America/Los_Angeles" );

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class
		};
	}

	@Override
	protected void addSettings(Map settings) {
		settings.put(
				AvailableSettings.CONNECTION_PROVIDER,
				connectionProvider
		);
	}

	@Override
	protected void releaseResources() {
		super.releaseResources();
		connectionProvider.stop();
	}

	@Test
	public void testTimeZone() {
		doInHibernate( this::sessionFactory, s -> {
			Person person = new Person();
			person.id = 1L;
			//Y2K
			person.createdOn = new Timestamp(946684800000L);
			s.persist( person );

		} );
		doInHibernate( this::sessionFactory, s -> {
			s.doWork( connection -> {
				try (Statement st = connection.createStatement()) {
					try (ResultSet rs = st.executeQuery(
							"SELECT " +
							"	to_char(createdon, 'YYYY-MM-DD HH24:MI:SS.US') " +
							"FROM person" )) {
						while ( rs.next() ) {
							String timestamp = rs.getString( 1 );
							assertEquals("1999-12-31 16:00:00.000000", timestamp);
						}
					}
				}
			} );
		} );
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		private Timestamp createdOn;
	}
}

