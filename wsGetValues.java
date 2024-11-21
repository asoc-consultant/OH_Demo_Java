
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.gson.Gson;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;



public class WsShipTimesDR extends HttpServlet {
    private Connection sqlConn;

    public WsShipTimesDR() {
        try {
            // Establish database connection
            sqlConn = DriverManager.getConnection("jdbc:sqlserver://10.1.12.22;databaseName=PB_reporting", "pbReporting_user", "kB60Wu^U9R7");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

public ResultSet getDataSet(String sqlQuery) {
    ResultSet resultSet = null;
    try {
        PreparedStatement statement = sqlConn.prepareStatement(sqlQuery);
        resultSet = statement.executeQuery();
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return resultSet;
}

public String secureString(String input) {
    if (input == null) {
        return null;
    }
    
    // Perform HTML encoding and custom replacements
    String output = input.replace("'", "&#39;")
                         .replace("&", "&amp;")
                         .replace("è", "&#232;")
                         .replace("É", "&#201;")
                         .replace("À", "&#192;")
                         .replace("à", "&#224;")
                         .replace("Ô", "&#212;")
                         .replace("ô", "&#244;")
                         .replace("Û", "&#219;");
    
    return output;
}

public String translateToFrench(String s) {
    String result = s;
    result = result.replace("January", "Janvier")
                   .replace("February", "Février")
                   .replace("March", "Mars")
                   .replace("April", "Avril")
                   .replace("May", "Mai")
                   .replace("June", "Juin");
    return result;
}

public List<Map<String, String>> getOrganizationByName(boolean addDefault, String text, String language) {
    String param = secureString(text);
    List<Map<String, String>> organizationList = new ArrayList<>();
    
    String query = "SELECT Org_ID, OrganizationName, " +
                   (language.equals("fr") ? "dl.Location_Label_French" : "dl.Location_Label") + " AS region, " +
                   "do.postalCode FROM DimOrganization do " +
                   "INNER JOIN DimOrgType dot ON do.OrgType_ID = dot.OrgType_ID " +
                   "INNER JOIN dimlocation dl ON do.Region_LocationID = dl.Location_ID " +
                   "WHERE dot.orgType_label = 'Warehouse Institution' AND WT_PublicReporting = 1 " +
                   "AND OrganizationName LIKE ? ORDER BY OrganizationName";
    
    try (PreparedStatement stmt = sqlConn.prepareStatement(query)) {
        stmt.setString(1, "%" + param + "%");
        ResultSet rs = stmt.executeQuery();
        
        while (rs.next()) {
            Map<String, String> row = new HashMap<>();
            row.put("Org_ID", rs.getString("Org_ID"));
            row.put("OrganizationName", rs.getString("OrganizationName"));
            row.put("region", rs.getString("region"));
            row.put("postalCode", rs.getString("postalCode"));
            organizationList.add(row);
        }
        
        if (addDefault) {
            Map<String, String> defaultRow = new HashMap<>();
            defaultRow.put("Org_ID", "-1");
            defaultRow.put("OrganizationName", "[Select Organization]");
            organizationList.add(0, defaultRow);
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    
    return organizationList;
}


public List<Map<String, String>> getCitiesByName(String text, String language) {
    String param = secureString(text);
    List<Map<String, String>> citiesList = new ArrayList<>();
    
    String query = "SELECT DISTINCT City_LocationID, dl.Location_Label AS city " +
                   "FROM DimOrganization do " +
                   "INNER JOIN DimOrgType dot ON do.OrgType_ID = dot.OrgType_ID " +
                   "INNER JOIN dimlocation dl ON do.City_LocationID = dl.Location_ID " +
                   "WHERE dot.orgType_label = 'Warehouse Institution' AND do.WT_PublicReporting = 1 " +
                   "AND dl.Location_Label LIKE ? ORDER BY dl.Location_Label";

    try (PreparedStatement stmt = sqlConn.prepareStatement(query)) {
        stmt.setString(1, "%" + param + "%");
        ResultSet rs = stmt.executeQuery();
        
        while (rs.next()) {
            Map<String, String> row = new HashMap<>();
            row.put("City_LocationID", rs.getString("City_LocationID"));
            row.put("city", rs.getString("city"));
            citiesList.add(row);
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    
    return citiesList;
}

public List<Map<String, String>> getAllOrganization(boolean addDefault, String language) {
    List<Map<String, String>> organizationList = new ArrayList<>();
    
    String query = "SELECT Org_ID, OrganizationName, " +
                   (language.equals("fr") ? "dl.Location_Label_French" : "dl.Location_Label") + " AS Region, " +
                   "dl2.Location_Label AS city, OrgCategory_Label AS HospType, do.postalCode " +
                   "FROM DimOrganization do " +
                   "INNER JOIN DimOrgType dot ON do.OrgType_ID = dot.OrgType_ID " +
                   "INNER JOIN DimOrganizationCategory doc ON doc.OrgCategory_ID = do.OrgCatID " +
                   "INNER JOIN dimlocation dl ON do.Region_LocationID = dl.Location_ID " +
                   "INNER JOIN dimlocation dl2 ON do.City_LocationID = dl2.Location_ID " +
                   "WHERE dot.orgType_label = 'Warehouse Institution' AND WT_PublicReporting = 1 " +
                   "ORDER BY OrganizationName";

    try (PreparedStatement stmt = sqlConn.prepareStatement(query)) {
        ResultSet rs = stmt.executeQuery();
        
        while (rs.next()) {
            Map<String, String> row = new HashMap<>();
            row.put("Org_ID", rs.getString("Org_ID"));
            row.put("OrganizationName", rs.getString("OrganizationName"));
            row.put("Region", rs.getString("Region"));
            row.put("city", rs.getString("city"));
            row.put("HospType", rs.getString("HospType"));
            row.put("postalCode", rs.getString("postalCode"));
            organizationList.add(row);
        }
        
        if (addDefault) {
            Map<String, String> defaultRow = new HashMap<>();
            defaultRow.put("Org_ID", "-1");
            defaultRow.put("OrganizationName", "[Select Organization]");
            organizationList.add(0, defaultRow);
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    
    return organizationList;
}


public class OrganizationDataAccess {

    private Connection connection; // Assume this is initialized elsewhere

    public ResultSet getOrganizationByID(boolean addDefault, String text, String language) throws SQLException {
        String param = secureString(text);
        String sqlString;

        sqlString = "SELECT do.Org_ID, OrganizationName, " + 
                    (language.equals("fr") ? " dl1.Location_Label_French " : "dl1.Location_Label") + 
                    " AS Region, dl2.Location_Label AS city, " +
                    " do.postalCode, do.website, do.fax, doc.OrgCategory_Label AS WarehouseType " + 
                    " FROM DimOrganization do " +
                    " INNER JOIN DimOrgType dot ON do.OrgType_ID = dot.OrgType_ID " +
                    " INNER JOIN DimOrganizationCategory doc ON do.OrgCatID = doc.OrgCategory_ID " +
                    " INNER JOIN dimlocation dl1 ON do.Region_LocationID = dl1.Location_ID " +
                    " INNER JOIN dimlocation dl2 ON do.City_LocationID = dl2.Location_ID " +
                    " WHERE dot.orgType_label = 'Warehouse Institution' AND do.WT_PublicReporting = 1 AND do.Org_ID = " + param;

        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(sqlString);
        
        List<Map<String, Object>> resultList = new ArrayList<>();
        ResultSetMetaData rsmd = rs.getMetaData();
        int columnCount = rsmd.getColumnCount();

        while (rs.next()) {
            Map<String, Object> row = new HashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                row.put(rsmd.getColumnName(i), rs.getObject(i));
            }
            resultList.add(row);
        }

        if (addDefault) {
            Map<String, Object> defaultRow = new HashMap<>();
            defaultRow.put("Org_ID", "-1");
            defaultRow.put("OrganizationName", "[Select Organization]");
            resultList.add(0, defaultRow);
        }

        return convertToResultSet(resultList); // You will need to implement this method
    }

    public ResultSet getOrganizationFactsByID(String text, String datatable, String language) throws SQLException {
        String param = secureString(text);
        StringBuilder sqlSB = new StringBuilder();

        if (datatable.equals("Fact_PS_CDI")) {
            sqlSB.append("SELECT TOP 1 dtp.timeperiod_label, [Cases_reporting], CAST(ROUND([calc_Rate], 2) AS DECIMAL(28, 2)) AS calc_Rate, [Outbreak_Status] ");
        } else if (datatable.equals("Fact_PS_MRSA") || datatable.equals("Fact_PS_VRE")) {
            sqlSB.append("SELECT TOP 1 dtp.timeperiod_label, [Cases_reporting], CAST(ROUND([calc_Rate], 3) AS DECIMAL(28, 3)) AS calc_Rate ");
        } else if (datatable.equals("Fact_PS_CLI") || datatable.equals("Fact_PS_VAP")) {
            sqlSB.append("SELECT TOP 1 dtp.timeperiod_label, [Cases_reporting], CAST(ROUND([calc_Rate], 2) AS DECIMAL(28, 2)) AS calc_Rate ");
        } else if (datatable.equals("Fact_PS_SSI") || datatable.equals("Fact_PS_SSCL")) {
            sqlSB.append("SELECT TOP 1 dtp.timeperiod_label, CAST(ROUND([Percent], 2) AS DECIMAL(28, 2)) AS [Percent] ");
        } else if (datatable.equals("Fact_PS_HH")) {
            sqlSB.append("SELECT TOP 1 dtp.timeperiod_label, CAST(ROUND([Percent_before], 2) AS DECIMAL(28, 2)) AS [Percent_before], CAST(ROUND([Percent_after], 2) AS DECIMAL(28, 2)) AS [Percent_after] ");
        }

        sqlSB.append(" FROM ").append(datatable).append(" fps ");
        sqlSB.append(" INNER JOIN dimtimeperiod dtp ON fps.TimePeriod_id = dtp.TimePeriod_id ");
        sqlSB.append(" WHERE fps.Org_ID = ").append(param);
        sqlSB.append(" ORDER BY dtp.TimePeriod_Date DESC");

        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(sqlSB.toString());

        return rs; // Returning the ResultSet directly
    }

    private String secureString(String input) {
        // Implement your string sanitization here
        return StringUtils.replace(input, "'", "''"); // Example of simple SQL injection prevention
    }

    // You'll need a method to convert List<Map<String, Object>> back to ResultSet
    private ResultSet convertToResultSet(List<Map<String, Object>> data) {
        // Implement the conversion logic to create a ResultSet from the List of Maps
        // This could involve using a library or creating a mock ResultSet implementation
        return null; // Placeholder
    }
}


public class OrganizationDataAccess {

    private Connection connection; // Assume this is initialized elsewhere

    public ResultSet getAllOrganizationFactsByID(String text, String datatable, String language) throws SQLException {
        String param = secureString(text);
        StringBuilder sqlSB = new StringBuilder();

        switch (datatable) {
            case "Fact_PS_CDI":
                sqlSB.append("SELECT dtp.timeperiod_label, [Cases_reporting], CAST(ROUND([calc_Rate], 2) AS DECIMAL(28, 2)) AS calc_Rate, [Outbreak_Status] ");
                break;
            case "Fact_PS_CLI":
            case "Fact_PS_VAP":
                sqlSB.append("SELECT dtp.timeperiod_label, [Cases_reporting], CAST(ROUND([calc_Rate], 2) AS DECIMAL(28, 2)) AS calc_Rate ");
                break;
            case "Fact_PS_MRSA":
            case "Fact_PS_VRE":
                sqlSB.append("SELECT dtp.timeperiod_label, [Cases_reporting], CAST(ROUND([calc_Rate], 3) AS DECIMAL(28, 3)) AS calc_Rate ");
                break;
            case "Fact_PS_SSI":
            case "Fact_PS_SSCL":
                sqlSB.append("SELECT dtp.timeperiod_label, CAST(ROUND([Percent], 2) AS DECIMAL(28, 2)) AS [Percent] ");
                break;
            case "Fact_PS_HH":
                sqlSB.append("SELECT dtp.timeperiod_label, CAST(ROUND([Percent_before], 2) AS DECIMAL(28, 2)) AS [Percent_before], CAST(ROUND([Percent_after], 2) AS DECIMAL(28, 2)) AS [Percent_after] ");
                break;
        }

        sqlSB.append("FROM ").append(datatable).append(" fps ");
        sqlSB.append("INNER JOIN dimtimeperiod dtp ON fps.TimePeriod_id = dtp.TimePeriod_id ");
        sqlSB.append("WHERE fps.Org_ID = ").append(param).append(" ");
        sqlSB.append("ORDER BY dtp.TimePeriod_Date DESC");

        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(sqlSB.toString());

        return rs; // Returning the ResultSet directly
    }

    public ResultSet getOrganizationByRegion(boolean addDefault, String text, String language) throws SQLException {
        String param = secureString(text);
        String sqlString;

        sqlString = "SELECT Org_ID, OrganizationName, " + 
                    (language.equals("fr") ? " dl.Location_Label_French " : "dl.Location_Label") + 
                    " AS Region, dl2.Location_Label AS city, OrgCategory_Label AS HospType, do.postalCode " + 
                    "FROM DimOrganization do " +
                    "INNER JOIN DimOrgType dot ON do.OrgType_ID = dot.OrgType_ID " +
                    "INNER JOIN DimOrganizationCategory doc ON doc.OrgCategory_ID = do.OrgCatID " +
                    "INNER JOIN dimlocation dl ON do.Region_LocationID = dl.Location_ID " +
                    "INNER JOIN dimlocation dl2 ON do.City_LocationID = dl2.Location_ID " +
                    "WHERE dot.orgType_label = 'Warehouse Institution' AND do.WT_PublicReporting = 1 " +
                    "AND dl.location_code = 'Region' + " + param + " ORDER BY OrganizationName";

        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(sqlString);

        if (addDefault) {
            // Create a temporary result set to add default values
            // This could be implemented using a suitable data structure or library
            List<Map<String, Object>> resultList = new ArrayList<>();
            Map<String, Object> defaultRow = new HashMap<>();
            defaultRow.put("Org_ID", "-1");
            defaultRow.put("OrganizationName", "[Select Organization]");
            resultList.add(defaultRow);

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("Org_ID", rs.getString("Org_ID"));
                row.put("OrganizationName", rs.getString("OrganizationName"));
                row.put("Region", rs.getString("Region"));
                row.put("city", rs.getString("city"));
                row.put("HospType", rs.getString("HospType"));
                row.put("postalCode", rs.getString("postalCode"));
                resultList.add(row);
            }
            // Convert resultList back to ResultSet if necessary
            return convertToResultSet(resultList); // Implement this method to convert to ResultSet
        }

        return rs; // Return the ResultSet directly if not adding default
    }

    public ResultSet getOrganizationByWarehouseType(boolean addDefault, String text, String language) throws SQLException {
        String param = secureString(text);
        String sqlString;

        sqlString = "SELECT Org_ID, OrganizationName, " + 
                    (language.equals("fr") ? " dl.Location_Label_French " : "dl.Location_Label") + 
                    " AS Region, dl2.Location_Label AS city, OrgCategory_Label AS HospType, do.postalCode " + 
                    "FROM DimOrganization do " +
                    "INNER JOIN DimOrgType dot ON do.OrgType_ID = dot.OrgType_ID " +
                    "INNER JOIN DimOrganizationCategory doc ON doc.OrgCategory_ID = do.OrgCatID " +
                    "INNER JOIN dimlocation dl ON do.Region_LocationID = dl.Location_ID " +
                    "INNER JOIN dimlocation dl2 ON do.City_LocationID = dl2.Location_ID " +
                    "WHERE dot.orgType_label = 'Warehouse Institution' AND do.WT_PublicReporting = 1 " +
                    "AND doc.OrgCategory_ID = '" + param + "' ORDER BY OrganizationName";

        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(sqlString);

        if (addDefault) {
            // Similar handling as in getOrganizationByRegion
            List<Map<String, Object>> resultList = new ArrayList<>();
            Map<String, Object> defaultRow = new HashMap<>();
            defaultRow.put("Org_ID", "-1");
            defaultRow.put("OrganizationName", "[Select Organization]");
            resultList.add(defaultRow);

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("Org_ID", rs.getString("Org_ID"));
                row.put("OrganizationName", rs.getString("OrganizationName"));
                row.put("Region", rs.getString("Region"));
                row.put("city", rs.getString("city"));
                row.put("HospType", rs.getString("HospType"));
                row.put("postalCode", rs.getString("postalCode"));
                resultList.add(row);
            }
            return convertToResultSet(resultList); // Implement this method to convert to ResultSet
        }

        return rs; // Return the ResultSet directly if not adding default
    }

    private String secureString(String input) {
        // Implement your string sanitization here
        return input.replace("'", "''"); // Example of simple SQL injection prevention
    }

    // You'll need a method to convert List<Map<String, Object>> back to ResultSet
    private ResultSet convertToResultSet(List<Map<String, Object>> data) {
        // Implement the conversion logic to create a ResultSet from the List of Maps
        // This could involve using a library or creating a mock ResultSet implementation
        return null; // Placeholder
    }
}


public class OrganizationDataAccess {

    private Connection connection; // Assume this is initialized elsewhere

    public ResultSet getOrganizationByInd(String ind, String hospType) throws SQLException {
        String param = secureString(hospType);
        String indicator = secureString(ind);
        String columnName = "";
        StringBuilder sb = new StringBuilder();
        int roundAs = 2;

        // Determine the column name and rounding based on the indicator
        switch (ind.toLowerCase()) {
            case "cdi":
            case "cli":
            case "mrsa":
            case "vap":
            case "vre":
                columnName = "[calc_Rate]";
                break;
            case "ssi":
            case "sscl":
                columnName = "[Percent]";
                break;
            case "hh":
                columnName = "[Percent_after]";
                break;
        }

        if (ind.equalsIgnoreCase("mrsa") || ind.equalsIgnoreCase("vre")) {
            roundAs = 3;
        }

        // Query to get time periods
        sb.append("SELECT DISTINCT TOP 2 dtp.TimePeriod_Label, TimePeriod_Date FROM Fact_PS_").append(indicator).append(" fps ");
        sb.append("INNER JOIN DimTimePeriod dtp ON fps.timePeriod_id = dtp.timePeriod_id ");
        sb.append("ORDER BY TimePeriod_Date DESC");

        Statement stmt1 = connection.createStatement();
        ResultSet ds1 = stmt1.executeQuery(sb.toString());

        sb.setLength(0); // Clear the StringBuilder
        ResultSetMetaData rsmd = ds1.getMetaData();
        int columnCount = rsmd.getColumnCount();

        // Store time periods in a list for later use
        List<String> timePeriods = new ArrayList<>();
        while (ds1.next()) {
            timePeriods.add(ds1.getString("TimePeriod_Label"));
        }
        
        if (!timePeriods.isEmpty()) {
            String lastPeriod = timePeriods.get(timePeriods.size() - 1);
            sb.append("SELECT do.Org_ID, OrganizationName, ");

            for (String period : timePeriods) {
                sb.append("SUM(CASE timePeriod_Label WHEN '").append(period).append("' THEN CAST(ROUND(")
                  .append(columnName).append(", ").append(roundAs).append(") AS DECIMAL(28, ").append(roundAs).append(")) END) AS [").append(period).append("], ");
            }

            sb.append("FROM DimOrganization do ");
            sb.append("INNER JOIN DimOrgType dot ON do.OrgType_ID = dot.OrgType_ID ");
            sb.append("INNER JOIN DimOrganizationCategory doc ON do.OrgCatID = doc.OrgCategory_ID ");
            sb.append("INNER JOIN Fact_PS_").append(indicator).append(" fps ON fps.org_id = do.org_id ");
            sb.append("INNER JOIN DimTimePeriod dtp ON fps.timePeriod_id = dtp.timePeriod_id ");
            sb.append("WHERE dot.orgType_label = 'Warehouse Institution' AND do.WT_PublicReporting = 1 AND do.OrgCatID = ").append(param);
            sb.append("GROUP BY do.Org_ID, OrganizationName ");
            sb.append("ORDER BY OrganizationName ");

            Statement stmt2 = connection.createStatement();
            ResultSet ds = stmt2.executeQuery(sb.toString());

            return ds; // Returning the ResultSet directly
        }

        return null; // Return null if no time periods found
    }

    public ResultSet getOrganizationByPostalCode(boolean addDefault, String language) throws SQLException {
        String sqlString = "SELECT Org_ID, OrganizationName, dl.Location_Label AS city, " +
                (language.equals("fr") ? " dl.Location_Label_French " : "dl.Location_Label") + 
                " AS Region, do.postalCode, OrgCategory_Label AS HospType, longitude, latitude FROM DimOrganization do " +
                "INNER JOIN DimOrgType dot ON do.OrgType_ID = dot.OrgType_ID " +
                "INNER JOIN DimOrganizationCategory doc ON doc.OrgCategory_ID = do.OrgCatID " +
                "INNER JOIN dimlocation dl ON do.Region_LocationID = dl.Location_ID " +
                "WHERE dot.orgType_label = 'Warehouse Institution' AND do.WT_PublicReporting = 1 AND longitude IS NOT NULL AND latitude IS NOT NULL";

        Statement stmt = connection.createStatement();
        ResultSet ds = stmt.executeQuery(sqlString);

        // If addDefault is true, we can handle that here as well
        if (addDefault) {
            // Add a default row to the ResultSet if needed
            // Note: You may need to create a separate structure to accommodate this, as ResultSet is read-only
        }

        return ds; // Returning the ResultSet directly
    }

    public ResultSet getOrganizationByCity(String param, String language) throws SQLException {
        String sqlString = "SELECT Org_ID, OrganizationName, dl1.Location_Label AS city, dl2.Location_Label AS Region, OrgCategory_Label AS HospType, do.postalCode FROM DimOrganization do " +
                "INNER JOIN DimOrgType dot ON do.OrgType_ID = dot.OrgType_ID " +
                "INNER JOIN DimOrganizationCategory doc ON doc.OrgCategory_ID = do.OrgCatID " +
                "INNER JOIN dimlocation dl1 ON do.City_LocationID = dl1.Location_ID " +
                "INNER JOIN dimlocation dl2 ON do.Region_LocationID = dl2.Location_ID " +
                "WHERE dot.orgType_label = 'Warehouse Institution' AND do.WT_PublicReporting = 1 AND dl1.Location_Label = '" + param + "' " +
                "ORDER BY OrganizationName";

        Statement stmt = connection.createStatement();
        ResultSet ds = stmt.executeQuery(sqlString);

        return ds; // Returning the ResultSet directly
    }

    private String secureString(String input) {
        // Implement your security string logic here
        return input.replace("'", "''"); // Example to escape single quotes
    }

    // Add other helper methods as necessary
}


public class OrganizationDataAccess {

    private Connection connection; // Assume this is initialized elsewhere

    public ResultSet getPostalCode(String sPC) throws SQLException {
        String param = secureString(sPC);
        param = param.trim().replace("_", "").replace("-", "").replace(" ", "");
        param = param.substring(0, 3) + " " + param.substring(3); // Insert space after third character

        String sqlString = "SELECT Latitude, Longitude FROM PostalCodes WHERE PostalCode = '" + param + "'";
        Statement stmt = connection.createStatement();
        return stmt.executeQuery(sqlString);
    }

    public String getTableDataByName(String q, String language) throws SQLException {
        String param = secureString(q);
        StringBuilder sb = new StringBuilder();
        ResultSet orgs = getOrganizationByName(false, param, language); // Assuming this method returns a ResultSet

        sb.append("[");
        orgs.last(); // Move cursor to the last row
        int lastRow = orgs.getRow(); // Get last row index
        orgs.beforeFirst(); // Move cursor before the first row

        while (orgs.next()) {
            String orgId = orgs.getString("Org_ID");
            String orgName = orgs.getString("OrganizationName").replace("\"", "''");
            String region = orgs.getString("Region");
            String postalCode = orgs.getString("postalcode");

            if (orgs.getRow() != lastRow) {
                sb.append("[\"<a href='javascript:displayIndividualWarehouseInfo(").append(orgId).append(")'>").append(orgName).append("</a>\",\"").append(region).append("\",\"").append(postalCode).append("\"],");
            } else {
                sb.append("[\"<a href='javascript:displayIndividualWarehouseInfo(").append(orgId).append(")'>").append(orgName).append("</a>\",\"").append(region).append("\",\"").append(postalCode).append("\"]");
            }
        }
        sb.append("]");

        return sb.toString();
    }

    public String getTableDataAllWarehouses(String language) throws SQLException {
        StringBuilder sb = new StringBuilder();
        ResultSet orgs = getAllOrganization(false, language); // Assuming this method returns a ResultSet

        sb.append("[");
        orgs.last(); // Move cursor to the last row
        int lastRow = orgs.getRow(); // Get last row index
        orgs.beforeFirst(); // Move cursor before the first row

        while (orgs.next()) {
            String orgId = orgs.getString("Org_ID");
            String orgName = orgs.getString("OrganizationName").replace("\"", "''");
            String city = orgs.getString("city");
            String region = orgs.getString("Region");
            String hospType = orgs.getString("HospType");
            String postalCode = orgs.getString("postalCode");

            if (orgs.getRow() != lastRow) {
                sb.append("[\"<input type='checkbox' name='orgid").append(orgId).append("' id='orgid").append(orgId).append("' value='").append(orgId).append("' data-OrgID='").append(orgId).append("' data-postalCode='").append(postalCode).append("' data-WarehouseType='").append(hospType).append("' data-Region='").append(region).append("' data-city='").append(city).append("' data-name='").append(orgName).append("'>\",\"").append(orgName).append("\",\"").append(city).append("\",\"").append(region).append("\",\"").append(hospType).append("\"],");
            } else {
                sb.append("[\"<input type='checkbox' name='orgid").append(orgId).append("' id='orgid").append(orgId).append("' value='").append(orgId).append("' data-OrgID='").append(orgId).append("' data-postalCode='").append(postalCode).append("' data-WarehouseType='").append(hospType).append("' data-Region='").append(region).append("' data-city='").append(city).append("' data-name='").append(orgName).append("'>\",\"").append(orgName).append("\",\"").append(city).append("\",\"").append(region).append("\",\"").append(hospType).append("\"]");
            }
        }
        sb.append("]");

        return sb.toString();
    }

    // Assuming secureString is a method that performs some security operations on the input string
    private String secureString(String input) {
        // Implement the security logic here
        return input; // Placeholder return statement
    }

    // Placeholder methods to simulate fetching organizations
    private ResultSet getOrganizationByName(boolean addDefault, String name, String language) throws SQLException {
        // Implement the logic to fetch organization by name
        return null; // Placeholder return
    }

    private ResultSet getAllOrganization(boolean addDefault, String language) throws SQLException {
        // Implement the logic to fetch all organizations
        return null; // Placeholder return
    }
}

public class WarehouseService {

    public Warehouse getIndividualWarehouseInfo(String q, String language) {
        String param = secureString(q);
        Warehouse warehouse = null;
        String[] datatables = { "Fact_PS_CDI", "Fact_PS_CLI", "Fact_PS_HH", "Fact_PS_MRSA", "Fact_PS_SSCL", "Fact_PS_SSI", "Fact_PS_VAP", "Fact_PS_VRE" };
        
        DataTable org = getOrganizationByID(false, param, language);
        DataSet factDS = new DataSet();

        for (String datatable : datatables) {
            DataTable tbCopy = getOrganizationFactsByID(param, datatable, language).copy();
            factDS.addTable(tbCopy);
        }

        if (org.getRows().length > 0) {
            DataRow r1 = org.getRows()[0];
            warehouse = new Warehouse();
            warehouse.setID(r1.get("Org_ID").toString());
            warehouse.setName(r1.get("OrganizationName").toString());
            warehouse.setWarehouseType(r1.get("WarehouseType").toString());
            warehouse.setRegion(r1.get("region").toString());
            warehouse.setCity(r1.get("city").toString());
            warehouse.setPostalCode(r1.get("postalCode").toString());
            warehouse.setWebsite(r1.get("website").toString());
        }

        if (factDS.getTableCount() > 0) {
            if (factDS.getTable("Fact_PS_CDI") != null) {
                StringBuilder outbreakMessage = new StringBuilder();
                String outbreakStatus = factDS.getTable("Fact_PS_CDI").getRows()[0].get("Outbreak_Status").toString();
                if (!outbreakStatus.trim().isEmpty()) {
                    outbreakMessage.append("<div><span>Outbreak Status: ").append(outbreakStatus).append(", please contact the warehouse for more information</span></div>");
                }
                warehouse.setTblCdi(outbreakMessage.toString() + "<table><thead><tr><th scope='col'>Reporting Period</th><th scope='col'>Rate per 1,000 patient days</th><th scope='col'>Case count</th></tr></thead><tbody>");
                
                for (DataRow dr : factDS.getTable("Fact_PS_CDI").getRows()) {
                    warehouse.setTblCdi(warehouse.getTblCdi() + "<tr><td style='width:150px'>" + dr.get("timeperiod_label").toString() + "</td><td>" + replaceFactCodes(dr.get("calc_Rate").toString(), false) + "</td><td>" + replaceFactCodes(dr.get("Cases_reporting").toString(), false) + "</td></tr>");
                }
                
                warehouse.setTblCdi(warehouse.getTblCdi() + "</tbody><tfoot><tr><td colspan=4><a href='javascript:displayIndividualWarehouseInfoByInd(" + warehouse.getID() + ", \"CDI\")'>All reporting periods</a><br/><a href='./searchByWarehouseType.html?ht=" + convertHospType(warehouse.getWarehouseType()) + "&ind=cdi'>Compare with other warehouses of the same type</a><br/><a href='../../completeReports/cdi.xls'>Download Historical Data</a></td></tr></tfoot></table>");
            }

            // Filling MRSA data
            if (factDS.getTable("Fact_PS_MRSA") != null) {
                warehouse.setTblMrsa("<table><thead><tr><th scope='col'>Reporting Period</th><th scope='col'>Rate per 1,000 patient days</th><th scope='col'>Case count</th></tr></thead><tbody>");
                
                for (DataRow dr : factDS.getTable("Fact_PS_MRSA").getRows()) {
                    warehouse.setTblMrsa(warehouse.getTblMrsa() + "<tr><td style='width:150px'>" + dr.get("timeperiod_label").toString() + "</td><td>" + replaceFactCodes(dr.get("calc_Rate").toString(), false) + "</td><td>" + replaceFactCodes(dr.get("Cases_reporting").toString(), false) + "</td></tr>");
                }
                
                warehouse.setTblMrsa(warehouse.getTblMrsa() + "</tbody><tfoot><tr><td colspan=4><a href='javascript:displayIndividualWarehouseInfoByInd(" + warehouse.getID() + ", \"MRSA\")'>All reporting periods</a><br/><a href='./searchByWarehouseType.html?ht=" + convertHospType(warehouse.getWarehouseType()) + "&ind=mrsa'>Compare with other warehouses of the same type</a><br/><a href='../../completeReports/mrsa.xls'>Download Historical Data</a></td></tr></tfoot></table>");
            }            
        }
        return warehouse;
    }    

    public List<Warehouse> getCities(String q, String language) {
        String param = secureString(q);
        DataTable orgs = getCitiesByName(param, language);
        List<Warehouse> warehouseNames = new ArrayList<>();

        for (DataRow r : orgs.getRows()) {
            warehouseNames.add(new Warehouse(r.get("City_LocationID").toString(), r.get("city").toString()));
        }

        return warehouseNames;
    }

    public List<Warehouse> getWarehouseNamesByPostalCode(String q, String language) {
        String param = secureString(q);
        DataTable orgs = getOrganizationByPostalCode(false, language);
        List<Warehouse> warehouseNames = new ArrayList<>();

        for (DataRow r : orgs.getRows()) {
            warehouseNames.add(new Warehouse(r.get("Org_ID").toString(), r.get("OrganizationName").toString()));
        }

        return warehouseNames;
    }

    public List<Warehouse> getWarehouseNamesByCity(String q, String language) {
        String param = secureString(q);
        DataTable orgs = getOrganizationByCity(param, language);
        List<Warehouse> warehouseNames = new ArrayList<>();

        for (DataRow r : orgs.getRows()) {
            warehouseNames.add(new Warehouse(r.get("Org_ID").toString(), r.get("OrganizationName").toString()));
        }

        return warehouseNames;
    }
}


public class WarehouseService {

    public String getTableDataByPostalCode(String q, String language) {
        String param = secureString(q);
        double longitude1 = 0;
        double latitude1 = 0;
        double distance;
        double longitude2 = 0;
        double latitude2 = 0;
        StringBuilder sb = new StringBuilder();

        DataTable sourcePC = getPostalCode(param);

        if (sourcePC.getRowCount() > 0) {
            longitude1 = Double.parseDouble(sourcePC.getRow(0).get("Longitude").toString());
            latitude1 = Double.parseDouble(sourcePC.getRow(0).get("Latitude").toString());

            DataTable orgs = getOrganizationByPostalCode(false, language);
            List<Warehouse> warehouses = new ArrayList<>();

            sb.append("[");
            DataRow lastRow = orgs.getRow(orgs.getRowCount() - 1);
            for (DataRow r : orgs.getRows()) {
                if (!r.get("longitude").toString().isEmpty() && !r.get("latitude").toString().isEmpty()) {
                    try {
                        longitude2 = Double.parseDouble(r.get("longitude").toString());
                        latitude2 = Double.parseDouble(r.get("latitude").toString());
                    } catch (NumberFormatException ex) {
                        // Handle exception as needed
                    }

                    distance = PS_DistanceAlgorithm.calcDistanceBetweenPlaces(latitude1, longitude1, latitude2, longitude2);
                    distance = Math.round(Math.sqrt(Math.pow(distance, 2) / 2) * 2 * 10.0) / 10.0; // Distance correction from straight distance to Manhattan distance.

                    warehouses.add(new Warehouse(r.get("Org_ID").toString(), 
                                                  r.get("city").toString(), 
                                                  r.get("OrganizationName").toString().replace("\"", "''"), 
                                                  r.get("region").toString(), 
                                                  r.get("HospType").toString(), 
                                                  r.get("postalcode").toString(), 
                                                  distance));
                }
            }

            warehouses.sort((x, y) -> {
                if (x == null) return (y == null) ? 0 : -1;
                if (y == null) return 1;
                return Double.compare(x.getDistanceToPostalCode(), y.getDistanceToPostalCode());
            });

            for (int i = 0; i < Math.min(20, warehouses.size()); i++) {
                String orgID = warehouses.get(i).getID();
                sb.append("[\"<input type='checkbox' name='orgid").append(orgID)
                    .append("' id='orgid").append(orgID)
                    .append("' value='").append(orgID)
                    .append("' data-OrgID='").append(orgID)
                    .append("' data-postalCode='").append(warehouses.get(i).getPostalCode())
                    .append("' data-WarehouseType='").append(warehouses.get(i).getWarehouseType())
                    .append("' data-region='").append(warehouses.get(i).getRegion())
                    .append("' data-city='").append(warehouses.get(i).getCity())
                    .append("' data-name='").append(warehouses.get(i).getName().replace("\"", "''"))
                    .append(">'\",").append("\"").append(warehouses.get(i).getName().replace("\"", "''"))
                    .append("\",").append("\"").append(warehouses.get(i).getRegion())
                    .append("\",").append("\"").append(warehouses.get(i).getWarehouseType())
                    .append("\",").append("\"").append(warehouses.get(i).getDistanceToPostalCode()).append("\"],");
            }

            sb.setLength(sb.length() - 1); // Remove the last comma
            sb.append("]");
        }
        return sb.toString();
    }

    public String getTableDataByWarehouseType(String q, String language) {
        String param = secureString(q);
        StringBuilder sb = new StringBuilder();
        DataTable orgs = getOrganizationByWarehouseType(false, param, language);
        List<Warehouse> warehouseNames = new ArrayList<>();

        sb.append("[");

        DataRow lastRow = null;
        boolean orgRowCount = orgs.getRowCount() > 0;
        if (orgRowCount) {
            lastRow = orgs.getRow(orgs.getRowCount() - 1);
        }

        if (orgRowCount) {
            for (DataRow r : orgs.getRows()) {
                String orgID = r.get("Org_ID").toString();
                if (!r.equals(lastRow)) {
                    sb.append("[\"<input type='checkbox' name='orgid").append(orgID)
                        .append("' id='orgid").append(orgID)
                        .append("' value='").append(orgID)
                        .append("' data-OrgID='").append(orgID)
                        .append("' data-postalCode='").append(r.get("postalCode").toString())
                        .append("' data-WarehouseType='").append(r.get("HospType").toString())
                        .append("' data-region='").append(r.get("region").toString())
                        .append("' data-city='").append(r.get("city").toString())
                        .append("' data-name='").append(r.get("OrganizationName").toString().replace("\"", "''"))
                        .append(">'\",").append("\"").append(r.get("OrganizationName").toString().replace("\"", "''"))
                        .append("\",").append("\"").append(r.get("city").toString())
                        .append("\",").append("\"").append(r.get("region").toString())
                        .append("\",").append("\"").append(r.get("HospType").toString()).append("\"],");
                } else {
                    sb.append("[\"<input type='checkbox' name='orgid").append(orgID)
                        .append("' id='orgid").append(orgID)
                        .append("' value='").append(orgID)
                        .append("' data-OrgID='").append(orgID)
                        .append("' data-postalCode='").append(r.get("postalCode").toString())
                        .append("' data-WarehouseType='").append(r.get("HospType").toString())
                        .append("' data-region='").append(r.get("region").toString())
                        .append("' data-city='").append(r.get("city").toString())
                        .append("' data-name='").append(r.get("OrganizationName").toString().replace("\"", "''"))
                        .append(">'\",").append("\"").append(r.get("OrganizationName").toString().replace("\"", "''"))
                        .append("\",").append("\"").append(r.get("city").toString())
                        .append("\",").append("\"").append(r.get("region").toString())
                        .append("\",").append("\"").append(r.get("HospType").toString()).append("\"]");
                }
            }
        }

        sb.append("]");
        return sb.toString();
    }
}


@WebMethod
public String getTableDataByRegion(String q, String language) {
    String param = secureString(q);
    StringBuilder sb = new StringBuilder();
    DataTable orgs = getOrganizationByRegion(false, param, language);
    List<Warehouse> warehouseNames = new ArrayList<>();

    sb.append("[");

    DataRow lastRow = null;
    boolean orgRowCount = false;
    if (orgs.getRows().size() > 0) {
        lastRow = orgs.getRows().get(orgs.getRows().size() - 1);
        orgRowCount = true;
    }

    if (orgRowCount) {
        for (DataRow r : orgs.getRows()) {
            String orgID = r.get("Org_ID").toString();
            if (!r.equals(lastRow)) {
                sb.append("[\"<input type='checkbox'  name='AAAorgid")
                        .append(orgID).append("' id='orgid")
                        .append(orgID).append("' value='")
                        .append(orgID).append("' data-OrgID='")
                        .append(orgID).append("' data-postalCode='")
                        .append(r.get("postalCode")).append("' data-WarehouseType='")
                        .append(r.get("HospType")).append("' data-region='")
                        .append(r.get("region")).append("' data-city='")
                        .append(r.get("city")).append("' data-name='")
                        .append(r.get("OrganizationName").toString().replace("\"", "''"))
                        .append(">\",\"").append(r.get("OrganizationName").toString().replace("\"", "''"))
                        .append("\",\"").append(r.get("city")).append("\",\"")
                        .append(r.get("region")).append("\",\"").append(r.get("HospType"))
                        .append("\"],");
            } else {
                sb.append("[\"<input type='checkbox'  name='AAAorgid")
                        .append(orgID).append("' id='orgid")
                        .append(orgID).append("' value='")
                        .append(orgID).append("' data-OrgID='")
                        .append(orgID).append("' data-postalCode='")
                        .append(r.get("postalCode")).append("' data-WarehouseType='")
                        .append(r.get("HospType")).append("' data-region='")
                        .append(r.get("region")).append("' data-city='")
                        .append(r.get("city")).append("' data-name='")
                        .append(r.get("OrganizationName").toString().replace("\"", "''"))
                        .append(">\",\"").append(r.get("OrganizationName").toString().replace("\"", "''"))
                        .append("\",\"").append(r.get("city")).append("\",\"")
                        .append(r.get("region")).append("\",\"").append(r.get("HospType"))
                        .append("\"]");
            }
        }
    }

    sb.append("]");

    return sb.toString();
}

@WebMethod
public String getTableDataByCity(String q, String language) {
    String param = secureString(q);
    StringBuilder sb = new StringBuilder();
    DataTable orgs = getOrganizationByCity(param, language);
    List<Warehouse> warehouseNames = new ArrayList<>();

    sb.append("[");
    DataRow lastRow = orgs.getRows().get(orgs.getRows().size() - 1);
    for (DataRow r : orgs.getRows()) {
        String orgID = r.get("Org_ID").toString();
        if (!r.equals(lastRow)) {
            sb.append("[\"<input type='checkbox'  name='AAAorgid")
                    .append(orgID).append("' id='orgid")
                    .append(orgID).append("' value='")
                    .append(orgID).append("' data-OrgID='")
                    .append(orgID).append("' data-postalCode='")
                    .append(r.get("postalCode")).append("' data-WarehouseType='")
                    .append(r.get("HospType")).append("' data-region='")
                    .append(r.get("region")).append("' data-city='")
                    .append(r.get("city")).append("' data-name='")
                    .append(r.get("OrganizationName").toString().replace("\"", "''"))
                    .append(">\",\"").append(r.get("OrganizationName").toString().replace("\"", "''"))
                    .append("\",\"").append(r.get("city")).append("\",\"")
                    .append(r.get("region")).append("\",\"").append(r.get("HospType"))
                    .append("\"],");
        } else {
            sb.append("[\"<input type='checkbox'  name='AAAorgid")
                    .append(orgID).append("' id='orgid")
                    .append(orgID).append("' value='")
                    .append(orgID).append("' data-OrgID='")
                    .append(orgID).append("' data-postalCode='")
                    .append(r.get("postalCode")).append("' data-WarehouseType='")
                    .append(r.get("HospType")).append("' data-region='")
                    .append(r.get("region")).append("' data-city='")
                    .append(r.get("city")).append("' data-name='")
                    .append(r.get("OrganizationName").toString().replace("\"", "''"))
                    .append(">\",\"").append(r.get("OrganizationName").toString().replace("\"", "''"))
                    .append("\",\"").append(r.get("city")).append("\",\"")
                    .append(r.get("region")).append("\",\"").append(r.get("HospType"))
                    .append("\"]");
        }
    }
    sb.append("]");

    return sb.toString();
}

@WebMethod
public String getTableDataByInd(String indicator, String q, String language) {
    StringBuilder sb = new StringBuilder();
    DataTable orgs = getOrganizationByInd(indicator, q);
    List<Warehouse> warehouseNames = new ArrayList<>();
    int i;

    sb.append("[");
    int columnsCount = orgs.getColumns().size();
    DataRow lastRow = orgs.getRows().get(orgs.getRows().size() - 1);

    sb.append("[\"").append(orgs.getColumns().get(2).getColumnName()).append("\"],[\"").append(orgs.getColumns().get(3).getColumnName()).append("\"],");

    for (DataRow r : orgs.getRows()) {
        if (!r.equals(lastRow)) {
            sb.append("[\"<a href='javascript:ByTopicDisplayIndividualWarehouseInfo(")
                    .append(r.get("Org_ID").toString()).append(")'>")
                    .append(r.get("OrganizationName").toString().replace("\"", "''")).append("</a>\"");
            for (i = 2; i < columnsCount; i++) {
                sb.append(",\"").append(r.get(i).equals(DBNull.VALUE) ? "Not Required to Report" : replaceFactCodes(r.get(i).toString(), true)).append("\"");
            }
            sb.append("],");
        } else {
            sb.append("[\"<a href='javascript:ByTopicDisplayIndividualWarehouseInfo(")
                    .append(r.get("Org_ID").toString()).append(")'>")
                    .append(r.get("OrganizationName").toString().replace("\"", "''")).append("</a>\"");
            for (i = 2; i < columnsCount; i++) {
                sb.append(",\"").append(r.get(i).equals(DBNull.VALUE) ? "Not Required to Report" : replaceFactCodes(r.get(i).toString(), true)).append("\"");
            }
            sb.append("]");
        }
    }
    sb.append("]");

    if (language.equals("fr")) {
        return translateToFrench(sb.toString());
    } else {
        return sb.toString();
    }
}


}
