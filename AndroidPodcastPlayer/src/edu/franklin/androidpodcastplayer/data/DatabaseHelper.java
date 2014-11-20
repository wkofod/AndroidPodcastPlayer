package edu.franklin.androidpodcastplayer.data;

import android.content.Context;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Creates a helper for creating and managing the SQLite database.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

	// Logcat tag
	private static final String LOG = "DatabaseHelper";

	// Database Version,
	//bump the version because we altered the episodes table
	//added podcast info table for repo related stuff
	private static final int DATABASE_VERSION = 3;
	// Database Name
	private static final String DATABASE_NAME = "PodcastPlayer.db";

	// final variables for tables and columns
	public static final String TABLE_PODCAST = "podcast";
	public static final String PODCAST_COLUMN_PODCASTID = "podcastId";
	public static final String PODCAST_COLUMN_NAME = "name";
	public static final String PODCAST_COLUMN_DESCRIPTION = "description";
	public static final String PODCAST_COLUMN_IMAGE = "image";
	public static final String PODCAST_COLUMN_NUMEPISODES = "numEpisodes";
	public static final String PODCAST_COLUMN_FEEDURL = "feedUrl";
	public static final String PODCAST_COLUMN_DIR = "dir";
	public static final String PODCAST_COLUMN_OLDESTFIRST = "oldestFirst";
	public static final String PODCAST_COLUMN_AUTODOWNLOAD = "autoDownload";
	public static final String PODCAST_COLUMN_AUTODELETE = "autoDelete";
	
	public static final String TABLE_PODCAST_INFO = "podcast_info";
	public static final String PC_INFO_ID = "pcId";
	public static final String PC_INFO_NAME = "name";
	public static final String PC_INFO_DESCRIPTION = "description";
	public static final String PC_INFO_URL = "url";
	public static final String PC_INFO_IMAGE_URL = "image_url";
	public static final String PC_INFO_POSITION = "position";

	public static final String TABLE_EPISODES = "episodes";
	public static final String EPISODES_COLUMN_EPISODEID = "episodeId";
	public static final String EPISODES_COLUMN_PODCASTID = "podcastId";
	public static final String EPISODES_COLUMN_NAME = "name";
	public static final String EPISODES_COLUMN_URL = "url";
	public static final String EPISODES_COLUMN_FILEPATH = "filepath";
	public static final String EPISODES_COLUMN_IMAGE = "image";
	public static final String EPISODES_COLUMN_TOTALTIME = "totalTime";
	public static final String EPISODES_COLUMN_PLAYEDTIME = "playedTime";
	public static final String EPISODES_COLUMN_NEW = "newEpisode";
	public static final String EPISODES_COLUMN_COMPLETED = "completed";

	public static final String TABLE_CONFIG = "config";
	public static final String CONFIG_COLUMN_CONFIGID = "configId";
	public static final String CONFIG_COLUMN_EXTSTORAGE = "extStorage";
	public static final String CONFIG_COLUM_WIFIONLY = "wifiOnly";

	// Table Create Statements
	// podcast table create statement
	private static final String CREATE_TABLE_PODCAST = "CREATE TABLE podcast(podcastId INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, " +
			"description TEXT, image TEXT, numEpisodes INTEGER, feedUrl TEXT, dir TEXT, oldestFirst INTEGER, autoDownload INTEGER, " +
			"autoDelete INTEGER)";
	//podcast info
	private static final String CREATE_TABLE_PC_INFO = "CREATE TABLE podcast_info(pcId INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT," +
			"description TEXT, url TEXT, image_url TEXT, position INTEGER)";
	// episodes table create statement
	private static final String CREATE_TABLE_EPISODES = "CREATE TABLE episodes(episodeId INTEGER PRIMARY KEY AUTOINCREMENT, " +
			"podcastId INTEGER, name TEXT, url TEXT, filepath TEXT, image TEXT, totalTime INTEGER, playedTime INTEGER, newEpisode INTEGER, " +
			"completed INTEGER)";
	// config table create statement
	private static final String CREATE_TABLE_CONFIG = "CREATE TABLE config(configId INTEGER PRIMARY KEY AUTOINCREMENT, " +
			"extStorage INTEGER, wifiOnly INTEGER)";

	// call superclass SQLiteOpenHelper constructor
	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	/**
	 * Creates the SQLite database.
	 */
	@Override
	public void onCreate(SQLiteDatabase db) {
		// creating required tables
		db.execSQL(CREATE_TABLE_PODCAST);
		Log.d(LOG, CREATE_TABLE_PODCAST);
		
		db.execSQL(CREATE_TABLE_PC_INFO);
		Log.d(LOG, CREATE_TABLE_PC_INFO);

		db.execSQL(CREATE_TABLE_EPISODES);
		Log.d(LOG, CREATE_TABLE_EPISODES);

		db.execSQL(CREATE_TABLE_CONFIG);
		Log.d(LOG, CREATE_TABLE_CONFIG);
	}

	/**
	 * Removes and recreates the database tables when the project is upgraded.
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// on upgrade drop older tables
		db.execSQL("DROP TABLE IF EXISTS podcast");
		db.execSQL("DROP TABLE IF EXISTS podcast_info");
		db.execSQL("DROP TABLE IF EXISTS episodes");
		db.execSQL("DROP TABLE IF EXISTS config");
		onCreate(db);
	}

	/**
	 * Close database
	 */
	public void closeDB() {
		SQLiteDatabase db = this.getReadableDatabase();
		if (db != null && db.isOpen())
			db.close();
	}
	
	public String escapeString(String value)
	{
		//sqlite appears to break on single quotes when inserting
		return value != null ? DatabaseUtils.sqlEscapeString(value) : "";
	}
	
	public String unescapeString(String value)
	{
		//if we have null or just an empty string, just return empty string
		if(value == null || value.equals("''")) return "";
		if(value.startsWith("'")) value = value.substring(1);
		if(value.endsWith("'")) value = value.substring(0, value.length() - 1);
		return value;
	}
}
