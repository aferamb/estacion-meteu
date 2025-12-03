package logic;

import Database.ConectionDDBB;
import java.util.ArrayList;
import java.util.Date;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Logic 
{
	public static ArrayList<Measurement> getDataFromDB()
	{
		ArrayList<Measurement> values = new ArrayList<Measurement>();
		
		ConectionDDBB conector = new ConectionDDBB();
		Connection con = null;
		try
		{
			con = conector.obtainConnection(true);
			Log.log.info("Database Connected");
			
			PreparedStatement ps = ConectionDDBB.GetDataBD(con);
			Log.log.info("Query=>" + ps.toString());
			ResultSet rs = ps.executeQuery();
			while (rs.next())
			{
				Measurement measure = new Measurement();
				measure.setValue(rs.getInt("VALUE"));
				measure.setDate(rs.getTimestamp("DATE"));
				values.add(measure);
			}	
		} catch (SQLException e)
		{
			Log.log.error("Error: " + e);
			values = new ArrayList<Measurement>();
		} catch (NullPointerException e)
		{
			Log.log.error("Error: " + e);
			values = new ArrayList<Measurement>();
		} catch (Exception e)
		{
			Log.log.error("Error:" + e);
			values = new ArrayList<Measurement>();
		}
		conector.closeConnection(con);
		return values;
	}

	public static ArrayList<Measurement> setDataToDB(int value)
	{
		ArrayList<Measurement> values = new ArrayList<Measurement>();
		
		ConectionDDBB conector = new ConectionDDBB();
		Connection con = null;
		try
		{
			con = conector.obtainConnection(true);
			Log.log.info("Database Connected");

			PreparedStatement ps = ConectionDDBB.SetDataBD(con);
			ps.setInt(1, value);
			// create the timestamp once so we can return the inserted row with the same time
			Timestamp now = new Timestamp((new Date()).getTime());
			ps.setTimestamp(2, now);
			Log.log.info("Query=>" + ps.toString());
			ps.executeUpdate();
			// return the inserted measurement so callers can confirm insertion
			Measurement m = new Measurement();
			m.setValue(value);
			m.setDate(now);
			values.add(m);
		} catch (SQLException e)
		{
			Log.log.error("Error: " + e);
			values = new ArrayList<Measurement>();
		} catch (NullPointerException e)
		{
			Log.log.error("Error: " + e);
			values = new ArrayList<Measurement>();
		} catch (Exception e)
		{
			Log.log.error("Error:" + e);
			values = new ArrayList<Measurement>();
		}
		conector.closeConnection(con);
		return values;
	}

	public static ArrayList<Measurement> getDataFromDate(String dateStr)
	{
		ArrayList<Measurement> values = new ArrayList<Measurement>();
		ConectionDDBB conector = new ConectionDDBB();
		Connection con = null;
		try
		{
			con = conector.obtainConnection(true);
			Log.log.info("Database Connected for date query: " + dateStr);
			// Expect dateStr in format YYYY-MM-DD
			String sql = "SELECT * FROM UBICOMP.MEASUREMENT WHERE DATE >= ? AND DATE <= ?";
			PreparedStatement ps = con.prepareStatement(sql);
			java.sql.Timestamp start = java.sql.Timestamp.valueOf(dateStr + " 00:00:00");
			java.sql.Timestamp end = java.sql.Timestamp.valueOf(dateStr + " 23:59:59");
			ps.setTimestamp(1, start);
			ps.setTimestamp(2, end);
			Log.log.info("Query=>" + ps.toString());
			ResultSet rs = ps.executeQuery();
			while (rs.next())
			{
				Measurement measure = new Measurement();
				measure.setValue(rs.getInt("VALUE"));
				measure.setDate(rs.getTimestamp("DATE"));
				values.add(measure);
			}
		} catch (SQLException e)
		{
			Log.log.error("Error: " + e);
			values = new ArrayList<Measurement>();
		} catch (NullPointerException e)
		{
			Log.log.error("Error: " + e);
			values = new ArrayList<Measurement>();
		} catch (Exception e)
		{
			Log.log.error("Error:" + e);
			values = new ArrayList<Measurement>();
		}
		conector.closeConnection(con);
		return values;
	}
	
	
}
