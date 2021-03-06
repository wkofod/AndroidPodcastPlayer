package edu.franklin.androidpodcastplayer;

import java.io.File;
import java.util.ArrayList;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import edu.franklin.androidpodcastplayer.data.ConfigData;
import edu.franklin.androidpodcastplayer.data.EpisodesData;
import edu.franklin.androidpodcastplayer.data.PodcastData;
import edu.franklin.androidpodcastplayer.data.SubscriptionData;
import edu.franklin.androidpodcastplayer.models.Podcast;
import edu.franklin.androidpodcastplayer.services.DownloadService;
import edu.franklin.androidpodcastplayer.services.RepositoryService;
import edu.franklin.androidpodcastplayer.services.SubscriptionService;

public class MainActivity extends ActionBarActivity {
	TableLayout table1;
	Context context = this;
	ConfigData configData = new ConfigData(context);
	PodcastData podcastData = new PodcastData(this);
	EpisodesData episodesData = new EpisodesData(this);
	SubscriptionData subData = new SubscriptionData(this, podcastData);
	ArrayList<Podcast> podcasts;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//initialize the download service
		setContentView(R.layout.activity_main);
		//call the initialize the singletons
		DownloadService.getInstance(this);
		//update the stuff we care about
		SubscriptionService.getInstance(this).updateSubscriptions();
		this.activateButton();
		table1 = (TableLayout) findViewById(R.id.table1);
		layoutTable();
	}
	
	private void layoutTable()
	{
		//out with the old
		int count = table1.getChildCount();
		for(int i = 0; i < count; i++)
		{
			table1.removeViewAt(0);
		}
		//create the header row
		this.addRow("header", "Title", "Saved","Auto", 2);
		//refresh podcast info
		podcasts = podcastData.getAllPodcasts();		
		if(podcasts.size()>0){
			for(Podcast currentPodcast: podcasts){
				String automaticallyDownload = subData.getSubscriptionById(currentPodcast.getPodcastId()).isAutoDownload() ? "Yes" : "No";
				
				//Handle null values
				String Url = currentPodcast.getImage()== null?"":currentPodcast.getImage();
				String name = currentPodcast.getName()== null?"":currentPodcast.getName();
				String savedEpisodes = currentPodcast.getEpisodes()== null?
						"":String.valueOf(currentPodcast.getNumEpisodes());
                //Use the values to populate the table
				this.addRow(Url,name ,savedEpisodes	,automaticallyDownload, 5);	
			}
		}
	}
	
	public void onResume()
	{
		Log.e("MAIN", "RESUME");
		configData.open();
		podcastData.open();
		subData.open();
		episodesData.open();
		layoutTable();
		super.onResume();
	}
	
	public void onStop()
	{
		Log.e("MAIN", "STOP");
		configData.close();
		podcastData.close();
		subData.close();
		episodesData.close();
		super.onStop();
	}
	
	public void onDestroy()
	{
		DownloadService.getInstance(this).close();
		SubscriptionService.getInstance(this).close();
		Log.i("Main", "Main Activity Destroyed");
		super.onDestroy();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		if (id == R.id.action_tests) {
			Intent intent = new Intent(this, TestsActivity.class);
			startActivity(intent);
		}
		if (id == R.id.action_home) {
			Intent intent = new Intent(this, MainActivity.class);
			startActivity(intent);
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void addRow(String imageUrl, String title, String saved,
			 String isAuto ,int id){
		
		TableRow row = new TableRow(this);
		row.setId(id);
		row.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT));		
		
		ImageView label_icon = new ImageView(this);
		label_icon.setId(20+id);		
		String imagePath = imageUrl;
		//load the image using an image loader
		if(imagePath.length() > 0 && !imagePath.equals("header") )
		{
			Picasso.with(this).load(new File(imagePath)).
			resize(100, 100).centerCrop().into(label_icon);
		}
		else if(imagePath.equals("header")){
			//Do nothing.No image, this is a header			
		}
		else{
			int imageResource = getResources().getIdentifier("@drawable/droid", null, getPackageName());
			Drawable res = getResources().getDrawable(imageResource);
			label_icon.setImageDrawable(res);
		}
		label_icon.setPadding(5, 5, 5, 0); // set the padding 
		row.addView(label_icon);// add the column to the table row here


		TextView label_title = new TextView(this);
		label_title.setId(21+id);// define id that must be unique
		String finaltitle = title.length()>20?title.substring(0,20):title;
		label_title.setText(finaltitle); // set the text 
		label_title.setTextColor(Color.WHITE); // set the color
		label_title.setPadding(5, 5, 5, 0); // set the padding 
		label_title.setTextSize(12);
		row.addView(label_title); // add the column to the table row 
		
		TextView label_saved = new TextView(this);
		label_saved.setId(21+id);// define id that must be unique
		label_saved.setText(saved); // set the text 
		label_saved.setTextColor(Color.WHITE); // set the color
		label_saved.setPadding(5, 5, 5, 0); // set the padding 
		row.addView(label_saved); // add the column to the table row 
		
		TextView label_is_auto = new TextView(this);
		label_is_auto.setId(23+id);// define id that must be unique
		label_is_auto.setText(isAuto); // set the text for 
		label_is_auto.setTextColor(Color.WHITE); // set the color
		label_is_auto.setPadding(5, 5, 5, 0); // set the padding 
		row.addView(label_is_auto); // add the column to the table row 
		final String rowTitle = title;
		final Intent intent = new Intent(this, PodcastDetails.class);
		row.setOnClickListener(new OnClickListener() {				
			@Override
			public void onClick(View v) {
				intent.putExtra("podcastName",rowTitle);
				startActivity(intent);
			}
		});
		table1.addView(row);
	}
	private void activateButton(){
		 Button button = (Button)findViewById(R.id.bottom_button);
		final Intent intent = new Intent(this, RepositoryActivity.class);
		    button.setOnClickListener(new Button.OnClickListener() {
		        public void onClick(View v) {
		        	startActivity(intent);
		        	}
		});
	}
	
}
