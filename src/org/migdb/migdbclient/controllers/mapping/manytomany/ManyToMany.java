package org.migdb.migdbclient.controllers.mapping.manytomany;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.migdb.migdbclient.config.FilePath;
import org.migdb.migdbclient.resources.ManyToManyResource;
import org.migdb.migdbclient.utils.ServiceAccessor;

import com.mongodb.util.JSON;

import scala.reflect.api.Trees.NewApi;

public class ManyToMany {

	JSONObject sqlJson;
	JSONArray manyToMany = new JSONArray();
	JSONObject mapped = new JSONObject();
	JSONArray mappedDataArray;
	JSONObject mappingMethod;

	public ManyToMany() {
		super();
		JSONParser parser = new JSONParser();
		JSONParser parser1 = new JSONParser();
		Object obj;
		Object obj1;
		try {
			obj = parser.parse(new FileReader(FilePath.DOCUMENT.getPath() + FilePath.DBSTRUCTUREFILE.getPath()));
			sqlJson = (JSONObject) obj;
			obj1 = parser1.parse(new FileReader(FilePath.DOCUMENT.getPath() + FilePath.COLLECTIONFILE.getPath()));
			mapped = (JSONObject) obj1;
		} catch (IOException | ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void identifyM2M() {

		JSONArray sqlTables = (JSONArray) sqlJson.get("tables");
		for (int i = 0; i < sqlTables.size(); i++) {
			int m2mCount = 0;
			JSONObject sqlTable = (JSONObject) sqlTables.get(i);
			if (sqlTable.containsKey("referencingFrom")) {
				JSONArray referencingFrom = (JSONArray) sqlTable.get("referencingFrom");
				if (referencingFrom.size() == 2) {

					for (int j = 0; j < referencingFrom.size(); j++) {
						JSONObject referenceInfo = (JSONObject) referencingFrom.get(j);
						String string = referenceInfo.get("relationshipType").toString();
						if (string.equals("ManyToMany")) {
							m2mCount++;
							// System.out.println(sqlTables.get(i));
						}
					}
				}
			}
			if (m2mCount == 2) {
				// manyToMany.add(sqlTable);
				// System.out.println(sqlTable);
				mappedDataArray = new JSONArray();
				setMapInstance(sqlTable);
				if (mappingMethod.get("method").toString().equals("EMBEDDING")) {
					embed();
				} else if (mappingMethod.get("method").toString().equals("REFERENCING")) {
					reference();
				}

				writeMappedJson();
			}
		}

	}

	public void setMapInstance(JSONObject mappingTable) {
		ManyToManyResource.INSTANCE.setMappingTable(mappingTable);
		mappingMethod = getMappingDecision();
		JSONArray referenceInfo = (JSONArray) mappingTable.get("referencingFrom");
		JSONObject object1 = (JSONObject) referenceInfo.get(0);
		String string1 = (String) object1.get("referencedTab");
		if (string1.equals(mappingMethod.get("parent").toString())) {

			ManyToManyResource.INSTANCE.setTable1Info(object1);
			JSONObject object2 = (JSONObject) referenceInfo.get(1);
			ManyToManyResource.INSTANCE.setTable2Info(object2);
			String string2 = (String) object2.get("referencedTab");
			System.out.println(string1 + string2);

			JSONArray sqlTables = (JSONArray) sqlJson.get("tables");

			for (int i = 0; i < sqlTables.size(); i++) {
				JSONObject table = (JSONObject) sqlTables.get(i);
				if (table.get("name").toString().equals(string1)) {
					ManyToManyResource.INSTANCE.setTable1(table);
				} else if (table.get("name").toString().equals(string2)) {
					ManyToManyResource.INSTANCE.setTable2(table);
				}
			}
		} else {
			ManyToManyResource.INSTANCE.setTable2Info(object1);
			JSONObject object2 = (JSONObject) referenceInfo.get(1);
			ManyToManyResource.INSTANCE.setTable1Info(object2);
			String string2 = (String) object2.get("referencedTab");
			System.out.println(string1 + string2);

			JSONArray sqlTables = (JSONArray) sqlJson.get("tables");

			for (int i = 0; i < sqlTables.size(); i++) {
				JSONObject table = (JSONObject) sqlTables.get(i);
				if (table.get("name").toString().equals(string1)) {
					ManyToManyResource.INSTANCE.setTable2(table);
				} else if (table.get("name").toString().equals(string2)) {
					ManyToManyResource.INSTANCE.setTable1(table);
				}
			}

		}
		// System.out.println(ManyToManyResource.INSTANCE.getTable1());
		// System.out.println(ManyToManyResource.INSTANCE.getTable2());

	}

	public JSONObject getMappingDecision() {

		JSONArray sqlTables = (JSONArray) sqlJson.get("tables");
		JSONObject mappingTable = ManyToManyResource.INSTANCE.getMappingTable();
		JSONArray referencingFrom = (JSONArray) mappingTable.get("referencingFrom");
		JSONObject referencingTable1Info = (JSONObject) referencingFrom.get(0);
		JSONObject referencingTable2Info = (JSONObject) referencingFrom.get(1);
		System.out.println("referencingFrom : " + referencingFrom);
		JSONObject referencingTable1 = new JSONObject();
		JSONObject referencingTable2 = new JSONObject();
		for (int i = 0; i < sqlTables.size(); i++) {
			JSONObject table = (JSONObject) sqlTables.get(i);
			if (table.get("name").toString().equals(referencingTable1Info.get("referencedTab"))) {
				referencingTable1 = (JSONObject) sqlTables.get(i);
			} else if (table.get("name").toString().equals(referencingTable2Info.get("referencedTab"))) {
				referencingTable2 = (JSONObject) sqlTables.get(i);
			}
		}
		ServiceAccessor accessor = new ServiceAccessor();
		JSONObject table1Response = accessor.getMappingModel("ClientId", "requestId","5","0","5","0");
		JSONObject table2Response = accessor.getMappingModel("ClientId", "requestId","3","1","2","0");
		JSONObject summary = new JSONObject();
		System.out.println("mapping request : " + table1Response + "\n" + table2Response);
		if (Double.parseDouble(table1Response.get("complexity").toString()) > Double
				.parseDouble(table2Response.get("complexity").toString())) {
			summary.put("parent", referencingTable1Info.get("referencedTab"));
			summary.put("method", table1Response.get("mappingModel"));

		} else {
			summary.put("parent", referencingTable2Info.get("referencedTab"));
			summary.put("method", table2Response.get("mappingModel"));

		}
		return summary;
	}

	public void embed() {
		JSONObject table1 = ManyToManyResource.INSTANCE.getTable1();

		if (!isTableMapped(table1)) {
			insertTable(table1);
		}
		setTable2();
		JSONArray table1Data = (JSONArray) table1.get("data");
		JSONObject mappingTable = ManyToManyResource.INSTANCE.getMappingTable();
		JSONObject table1MappingInfo = ManyToManyResource.INSTANCE.getTable1Info();
		JSONArray mappingData = (JSONArray) mappingTable.get("data");
		String referencingCol = table1MappingInfo.get("referencingCol").toString();
		String referencedCol = table1MappingInfo.get("referencedCol").toString();
		String referencedTable = table1MappingInfo.get("referencedTab").toString();
		JSONObject table2MappingInfo = ManyToManyResource.INSTANCE.getTable2Info();
		String referencingTable = table2MappingInfo.get("referencedTab").toString();

		for (Object object : table1Data) {
			JSONObject table1DataObject = (JSONObject) object;
			String table1Value = table1DataObject.get(referencingCol).toString();
			JSONArray referencingDataArray = new JSONArray();
			for (Object object2 : mappingData) {
				JSONObject mappingObject = (JSONObject) object2;
				String referencedValue = mappingObject.get(referencedCol).toString();
				if (referencedValue.equals(table1Value)) {
					JSONObject referencingObject = findTable2Value(mappingObject);
					// System.out.println(table1DataObject+"###"+referencingObject);
					referencingDataArray.add(referencingObject);
				}
			}
			// System.out.println(referencingDataArray);
			generateMappedDataArray(referencedTable, referencingTable, referencingCol, table1Value,
					referencingDataArray);
		}
		// insertToMapped(referencedTable);

		System.out.println("embed result : " + mapped);
	}

	public void setTable2() {
		JSONObject table = ManyToManyResource.INSTANCE.getTable2();
		if (isTableMapped(table)) {
			JSONArray array = (JSONArray) mapped.get("collections");
			for (int i = 0; i < array.size(); i++) {
				JSONObject object = (JSONObject) array.get(i);
				if (object.get("collectionName").equals(table.get("name"))) {
					ManyToManyResource.INSTANCE.setTable2(object);
					// System.out.println("!!!!!"+object);
				}
			}
		} else {
			JSONArray array = (JSONArray) sqlJson.get("tables");
			JSONArray tableData = new JSONArray();
			for (int i = 0; i < array.size(); i++) {
				JSONObject object = (JSONObject) array.get(i);
				if (object.get("name").equals(table.get("name"))) {
					tableData = (JSONArray) object.get("data");
				}
			}
			JSONObject newCollection = new JSONObject();
			newCollection.put("collectionName", table.get("name").toString());
			newCollection.put("values", tableData);
			ManyToManyResource.INSTANCE.setTable2(newCollection);
		}
	}

	public JSONObject findTable2Value(JSONObject mappingObject) {

		JSONObject table2 = ManyToManyResource.INSTANCE.getTable2();
		JSONArray table2Data = (JSONArray) table2.get("values");
		JSONObject table2MappingInfo = ManyToManyResource.INSTANCE.getTable2Info();
		String referencingCol = table2MappingInfo.get("referencingCol").toString();
		String referencedCol = table2MappingInfo.get("referencedCol").toString();
		String referencedValue = mappingObject.get(referencedCol).toString();
		for (Object object : table2Data) {
			JSONObject table2DataObject = (JSONObject) object;
			String table2Value = table2DataObject.get(referencingCol).toString();
			if (referencedValue.equals(table2Value)) {
				return table2DataObject;
			}
		}

		return null;
	}

	public void generateMappedDataArray(String referencedCollection, String referencingTable, String referencingCol,
			String referencingColData, JSONArray referencingDataArray) {
		JSONArray mappedData = (JSONArray) mapped.get("collections");
		// JSONArray array = new JSONArray();
		for (int i = 0; i < mappedData.size(); i++) {
			JSONObject mappedCollection = (JSONObject) mappedData.get(i);
			String collectionName = mappedCollection.get("collectionName").toString();
			if (referencedCollection.equals(collectionName)) {
				JSONArray mappedValues = (JSONArray) mappedCollection.get("values");
				for (int j = 0; j < mappedValues.size(); j++) {
					JSONObject mappedDataObject = (JSONObject) mappedValues.get(j);
					String mappedDataValue = (String) mappedDataObject.get(referencingCol);
					if (mappedDataValue.equals(referencingColData)) {
						mappedDataObject.put(referencingTable, referencingDataArray);
						// mappedDataArray.add(mappedDataObject);
					}
				}
				// mappedCollection.put(referencingTable, referencingDataArray);
			}

		}

	}

	public void reference() {
		JSONObject table1 = ManyToManyResource.INSTANCE.getTable1();

		if (!isTableMapped(table1)) {
			insertTable(table1);
		}
		insertReferencedTable();
		JSONObject table2 = ManyToManyResource.INSTANCE.getTable2();

		JSONArray table1Data = (JSONArray) table1.get("data");
		JSONObject table1MappingInfo = ManyToManyResource.INSTANCE.getTable1Info();
		String referencingCol = table1MappingInfo.get("referencingCol").toString();
		String referencedCol = table1MappingInfo.get("referencedCol").toString();
		String referencedTable = table1MappingInfo.get("referencedTab").toString();
		JSONObject mappingTable = ManyToManyResource.INSTANCE.getMappingTable();
		JSONObject table2MappingInfo = ManyToManyResource.INSTANCE.getTable2Info();
		String referencingTable = table2MappingInfo.get("referencedTab").toString();

		JSONArray mappingData = (JSONArray) mappingTable.get("data");

		for (Object object : table1Data) {
			JSONObject table1DataObject = (JSONObject) object;
			String table1Value = table1DataObject.get(referencingCol).toString();
			JSONArray referencingIdArray = new JSONArray();
			for (Object object2 : mappingData) {
				JSONObject mappingObject = (JSONObject) object2;
				String referencedValue = mappingObject.get(referencedCol).toString();
				if (referencedValue.equals(table1Value)) {
					System.out.println(mappingObject + "@@@" + table1DataObject);
					JSONObject referencingId = findReferencedId(mappingObject);
					referencingIdArray.add(referencingId);
				}
			}
			System.out.println(referencingIdArray);
			generateReferencedDataArray(referencedTable, referencingTable, referencingCol, table1Value,
					referencingIdArray);
		}

	}

	public void generateReferencedDataArray(String referencedCollection, String referencingTable, String referencingCol,
			String referencingColData, JSONArray referencingDataArray) {
		JSONArray mappedData = (JSONArray) mapped.get("collections");
		for (int i = 0; i < mappedData.size(); i++) {
			JSONObject mappedCollection = (JSONObject) mappedData.get(i);
			String collectionName = mappedCollection.get("collectionName").toString();
			if (referencedCollection.equals(collectionName)) {
				JSONArray mappedValues = (JSONArray) mappedCollection.get("values");
				for (int j = 0; j < mappedValues.size(); j++) {
					JSONObject mappedDataObject = (JSONObject) mappedValues.get(j);
					String mappedDataValue = (String) mappedDataObject.get(referencingCol);
					if (mappedDataValue.equals(referencingColData)) {
						mappedDataObject.put(referencingTable, referencingDataArray);
						System.out.println(collectionName);
					}
				}
			}

		}
		System.out.println("referenced " + mapped);

	}

	public JSONObject findReferencedId(JSONObject mappingObject) {
		JSONObject table2 = ManyToManyResource.INSTANCE.getTable2();
		JSONArray table2Data = (JSONArray) table2.get("values");
		JSONObject table2MappingInfo = ManyToManyResource.INSTANCE.getTable2Info();
		String referencingCol = table2MappingInfo.get("referencingCol").toString();
		String referencedCol = table2MappingInfo.get("referencedCol").toString();
		String referencedValue = mappingObject.get(referencedCol).toString();
		for (Object object : table2Data) {
			JSONObject table2DataObject = (JSONObject) object;
			String table2Value = table2DataObject.get(referencingCol).toString();
			if (referencedValue.equals(table2Value)) {
				JSONObject referencedObjectId = new JSONObject();
				referencedObjectId.put("_id", table2DataObject.get("_id"));
				// System.out.println(referencedObjectId);
				return referencedObjectId;
			}
		}
		return null;
	}

	public void insertReferencedTable() {
		JSONObject table = ManyToManyResource.INSTANCE.getTable2();

		if (isTableMapped(table)) {
			JSONArray array = (JSONArray) mapped.get("collections");
			for (int i = 0; i < array.size(); i++) {
				JSONObject object = (JSONObject) array.get(i);
				if (object.get("collectionName").equals(table.get("name"))) {
					JSONArray tableData = (JSONArray) object.get("values");
					for (int j = 0; j < tableData.size(); j++) {
						JSONObject tableValue = (JSONObject) tableData.get(j);
						if (!tableData.contains("_id")) {
							ObjectId id = new ObjectId();
							tableValue.put("_id", id.toString());
						}
					}
					ManyToManyResource.INSTANCE.setTable2(object);
					// System.out.println("@@@"+object);
				}

			}
			mapped.put("collections", array);
			// System.out.println(mapped);
		} else {
			JSONArray array = (JSONArray) sqlJson.get("tables");
			JSONArray newDataArray = new JSONArray();
			for (int i = 0; i < array.size(); i++) {
				JSONObject object = (JSONObject) array.get(i);
				if (object.get("name").equals(table.get("name"))) {
					JSONArray tableData = (JSONArray) object.get("data");
					for (int j = 0; j < tableData.size(); j++) {
						JSONObject tableValue = (JSONObject) tableData.get(j);
						ObjectId id = new ObjectId();
						tableValue.put("_id", id.toString());
						newDataArray.add(tableValue);

					}
				}

			}
			JSONObject newCollection = new JSONObject();
			newCollection.put("collectionName", table.get("name").toString());
			newCollection.put("values", newDataArray);
			ManyToManyResource.INSTANCE.setTable2(newCollection);
			// System.out.println("###"+newCollection);
			JSONArray mappedArray = (JSONArray) mapped.get("collections");
			mappedArray.add(newCollection);
			System.out.println(mapped);

		}
	}

	public void writeMappedJson() {
		FileWriter file;
		try {
			file = new FileWriter(FilePath.DOCUMENT.getPath() + FilePath.COLLECTIONFILE.getPath());
			file.write(mapped.toJSONString());
			file.flush();
			file.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean isTableMapped(JSONObject table) {
		JSONArray array = (JSONArray) mapped.get("collections");
		boolean isExist = false;
		for (int i = 0; i < array.size(); i++) {
			JSONObject object = (JSONObject) array.get(i);
			if (object.get("collectionName").equals(table.get("name"))) {
				// System.out.println(table.get("name"));
				isExist = true;
			}

		}
		return isExist;
	}

	public void insertTable(JSONObject table) {
		JSONObject newCollection = new JSONObject();
		newCollection.put("collectionName", table.get("name"));
		JSONArray collectionData = new JSONArray();
		collectionData = (JSONArray) table.get("data");
		newCollection.put("values", collectionData);
		JSONArray array = (JSONArray) mapped.get("collections");
		array.add(newCollection);
		// ManyToManyResource.INSTANCE.setTable1(newCollection);
		mapped.put("collections", array);

		// System.out.println(mapped);

	}

	/*
	 * public void insertToMapped(String referencedCollection) { JSONArray
	 * mappedData = (JSONArray) mapped.get("collections"); JSONObject object =
	 * new JSONObject(); for (int i = 0; i < mappedData.size(); i++) {
	 * JSONObject mappedCollection = (JSONObject) mappedData.get(i); String
	 * collectionName = mappedCollection.get("collectionName").toString(); if
	 * (referencedCollection.equals(collectionName)) { //
	 * object.put("collectionName", referencedCollection); //
	 * object.put("values", mappedDataArray); }
	 * 
	 * } // mappedData.add(object); System.out.println("before" + mapped); //
	 * mapped.put("collections", mappedData); System.out.println("after" +
	 * mapped); FileWriter file; try { file = new
	 * FileWriter("C:\\Users\\Lakshan1\\Desktop\\Resources\\mapped.json");
	 * file.write(mapped.toJSONString()); file.flush(); file.close(); } catch
	 * (IOException e) { // TODO Auto-generated catch block e.printStackTrace();
	 * }
	 * 
	 * }
	 */

}
