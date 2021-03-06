package edu.franklin.androidpodcastplayer;

import java.util.HashMap;
import java.util.Map;

import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.DownloadManager.Request;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.Toast;
import edu.franklin.androidpodcastplayer.data.ConfigData;
import edu.franklin.androidpodcastplayer.data.EpisodesData;
import edu.franklin.androidpodcastplayer.data.PodcastData;
import edu.franklin.androidpodcastplayer.models.Channel;
import edu.franklin.androidpodcastplayer.models.Enclosure;
import edu.franklin.androidpodcastplayer.models.Episode;
import edu.franklin.androidpodcastplayer.models.Image;
import edu.franklin.androidpodcastplayer.models.Item;
import edu.franklin.androidpodcastplayer.models.Podcast;
import edu.franklin.androidpodcastplayer.models.Rss;
import edu.franklin.androidpodcastplayer.services.FileManager;

public class RssTestActivity extends ActionBarActivity 
{
	//the top level folder for the subscriptions
	private Context context = this;
	//we are going to ignore the config stuff for now...but a real
	//subscription would want to be able to set user preferences
	private ConfigData configData = new ConfigData(context);
	private PodcastData podcastData = new PodcastData(this);
	private EpisodesData episodesData = new EpisodesData(this);
	//need to save off the images and episodes we fetch
	private FileManager fileManager = null;
	private DownloadManager dm = null;
	//a map of queued downloads to thier file names
	Map<Long, String> downloadMap = new HashMap<Long, String>();
	
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_rss_test);
		//get a ref to the download manager
		dm = (DownloadManager)getSystemService(DOWNLOAD_SERVICE);
		fileManager = new FileManager(this);
		//listen for finishing downloads...
		registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
		//open up the data helpers into the database
		configData.open();
		podcastData.open();
		episodesData.open();
	}
	
	protected void onStop() 
	{
		//close down the db stuff?
		configData.close();
		podcastData.close();
		episodesData.close();
		unregisterReceiver(receiver);
		super.onStop();
	}
	
	// TODO - this is just a test to see if rss can be parse and
	// podcast/episode data can be added to the database and filesystem.
	// once the screens are created to handle browsing and subscribing to rss feeds
	// is ready, this stuff can be moved or deleted.
	public void subscribeToRawRssFeeds(View view)
	{
		try
		{
			//something to fetch the raw junk with
			Resources resources = getResources();
			//the ids we want to fetch
			int[] rawFeeds = new int[]{
				R.raw.coder_radio_rss,
				R.raw.java_posse_rss,
				R.raw.technophilia_rss
			};
			//now go over the feed ids and initialize an Rss object from the xml
			for(int id : rawFeeds)
			{
				Rss rss = new Rss();
				rss.initializeFromRaw(resources.openRawResource(id));
				Log.i("Raw Rss Test", "Got back an Rss object!\n" + rss.toString());
				//for the demo, we just assume a subscription
				subscribeToRss(rss);			
			}
		}
		catch(Exception e)
		{
			Log.e("Raw RSS", "Could not load the raw rss stuff", e);
		}
	}
	
	private void subscribeToRss(Rss rss)
	{
		//first things first, grab an image for this guy
		Channel channel = rss.getChannel();
		Image image = channel.getImage();
		String podcastTitle = channel.getTitle();
		String podcastHomeDir = Podcast.getPodcastDirectory(podcastTitle);
		Podcast pc = new Podcast();
		//fetch the actual image from the webs
		if(image != null)
		{
			String imageName = image.getUrl().substring(image.getUrl().lastIndexOf("/") + 1);
			downloadFile(Podcast.IMAGES, imageName, image.getUrl());
			Log.d("Raw Rss Sub", "Fetching an " + imageName + " at " + image.getUrl());
			pc.setImage(fileManager.getAbsoluteFilePath(Podcast.IMAGES, imageName));
		}
		//or just make a dir for this podcast
		else
		{
			fileManager.mkDir(podcastHomeDir);
		}
		Log.d("Raw Rss Sub", podcastTitle + " saved to " + fileManager.getAbsoluteFilePath(podcastHomeDir, null));
		//while we are at it, grab an episode as well?
		//TODO, fetch an image maybe...but that could take a while
		//either way, make an entry in the database for this podcast
		pc.setName(podcastTitle);
		pc.setDescription(channel.getDescription());
		pc.setNumEpisodes(0L);
		pc.setFeedUrl(channel.getLink());
		pc.setDir(fileManager.getAbsoluteFilePath(podcastHomeDir, null));
		//that should be enough to persist this guy
		pc = podcastData.createPodcast(pc);
		if(pc != null)
		{
			Toast.makeText(getApplicationContext(), podcastTitle + " is in the database!", Toast.LENGTH_SHORT).show();
			//the podcast is in the db...add in the episode info
			for(Item item : channel.getItemList())
			{
				Episode e = new Episode();
				e.setPodcastId(pc.getPodcastId());
				e.setCompleted(false);
				
				//item objects don't have images
				e.setImage("");
				e.setName(item.getTitle());
				String link = item.getLink();
				//if there is an enclosure, use that for the url
				if(item.getEnclosure() != null)
				{
					Enclosure enc = item.getEnclosure();
					link = enc.getUrl().length() > 0 ? enc.getUrl() : link;
				}
				e.setUrl(link);
				//we haven't downloaded it yet
				e.setFilepath("");
				e.setNewEpisode(true);
				e.setPlayedTime(0);
				//Use the duration if it was provided by the file.
				e.setTotalTime(item.getDuration());
				e = episodesData.createEpisode(e);
				if(e != null)
				{
					pc.addEpisode(e);
					Log.d("Raw Rss Sub", "Added an episode for " + pc.getName() + ", called " + e.getName() + " into the database!");
				}
				else
				{
					Log.d("Raw Rss Sub", "Episode could not be added to the database !");
				}
			}
			Toast.makeText(getApplicationContext(), podcastTitle + " now has " + pc.getEpisodes().size() + " episodes in the database!", Toast.LENGTH_SHORT).show();
		}
		else
		{
			Toast.makeText(getApplicationContext(), podcastTitle + " could not be inserted into the database! Check logs...", Toast.LENGTH_LONG).show();
		}
		
	}
	
	public void downloadFile(String dir, String file, String url)
	{
		Log.d("Rss------", "Downloading " + dir + ":" + file + " located at : " + url);
		Uri uri = Uri.parse(url);
		Request request = new Request(uri);
        Long queuedId = Long.valueOf(dm.enqueue(request));
        downloadMap.put(queuedId, dir + ":" + file);
	}

	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		
		if (id == R.id.action_settings) 
		{
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	public void fetchRss(View view)
	{
		int duration = Toast.LENGTH_SHORT;
		//grab the text box and get its text
		EditText editText = (EditText) findViewById(R.id.urlField);
		//get its text
		String url = editText.getText().toString();
		//show what we are doing
		Toast.makeText(getApplicationContext(), "Fetching Rss at " + url, duration).show();
		try
		{
			Rss rss = new Rss();
			rss.initializeFromUrl(url);
			subscribeToRss(rss);
			WebView wv = (WebView)findViewById(R.id.webView1);
			wv.loadData(rss.toHtml(url), "text/html", null);
			
		}
		catch(Exception e)
		{
			Log.e("RSS", "Could not parse the RSS!", e);
			Toast.makeText(getApplicationContext(), 
					"Rss could not be parsed. Are you sure the url is valid? Check LogCat for problems.", 
					Toast.LENGTH_LONG).show();
		}
	}
	
	private void showDownloadStatus(String filename, boolean success)
	{
		Toast.makeText(getApplicationContext(), success ? 
			filename + " has been downloaded!" :
			filename + " failed to download. Checl the logs to see what blew up.", 
			Toast.LENGTH_SHORT).show();
	}
	
	private BroadcastReceiver receiver = new BroadcastReceiver() 
	{
		public void onReceive(Context context, Intent intent) 
		{
			String action = intent.getAction();
			Log.d("Rss Sub Download", "Got back an action " + action);
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                if(downloadMap.get(Long.valueOf(downloadId)) == null)
                {
                	return;
                }
                Query query = new Query();
                query.setFilterById(downloadId);
                Cursor c = dm.query(query);
                //anything to look at?
                if(c.getCount() > 0)
                {
                	//set initial cursor spot
                	c.moveToFirst();
            		int statusIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    int status = c.getInt(statusIndex);
                    int fileLocationIndex = c.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME);
                    String fileLocation = c.getString(fileLocationIndex);
                    Log.d("Rss Sub Download", "Status of " + fileLocation + " is " + status);
                    if (DownloadManager.STATUS_SUCCESSFUL == status) 
                    {
                    	String fileInfo = downloadMap.get(Long.valueOf(downloadId));
                    	if(fileInfo != null)
                    	{
                    		String[] tokens = fileInfo.split(":");
                    		String dir = tokens[0];
                    		String file = tokens[1];
                    		//put the downloaded file into our storage
                    		boolean moved = fileManager.moveFile(fileLocation, dir, file);
                    		Log.d("Rss Sub Download", 
                    				fileLocation + " moved to " + 
            						fileManager.getAbsoluteFilePath(dir, file) + " = " + moved);
                    		//remove this entry from the ones we are waiting on...it is done
                        	downloadMap.remove(downloadId);
                        	showDownloadStatus(file, moved);
                        	//either way, remove the file from the download manager because
                        	//it is done.
                        	dm.remove(downloadId);
                    	}
                    }
                }
                else
                {
                	Log.d("Rss Sub Download", "Got a download event, but no rows were returned");
                }
                //now close up the cursor
                c.close();
            }
		}
	};
}
