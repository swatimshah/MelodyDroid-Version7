package com.droid.melodydroid.display;

import com.droid.melodydroid.R;
import com.droid.melodydroid.core.DatabaseSynchronizationService;
import com.droid.melodydroid.core.MDSInterface;
import com.droid.melodydroid.core.MDService;
import com.droid.melodydroid.core.MelodyDroidApplication;
import com.droid.melodydroid.criteria.SearchCriteria;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.OnGestureListener;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class DisplayPlaybackControls extends Activity implements
		OnGestureListener {

	private static MDSInterface mpInterface;
	private static MediaPlayer mp = new MediaPlayer();
	static SeekBar sb;
	int position;
	String screen;
	private static String playPause = null;
	private static TextView myMarquee;
	private static ImageView albumArt;
	private static TextView duration;
	private static TextView currentTime;
	private static String durationString;
	private static String progressString;
	private static LinearLayout pbll;
	private static ImageView pauseImg;
	private static ImageView playImg;
	private static ImageView skipbImg;
	private static ImageView skipfImg;
	private static ImageView stopImg;
	private String swipeBackVar;
	private static final int SWIPE_MIN_DISTANCE = 120;
	private static final int SWIPE_MAX_OFF_PATH = 250;
	private static final int SWIPE_THRESHOLD_VELOCITY = 200;
	private GestureDetector gestureDetector;
	View.OnTouchListener gestureListener;
	private static String playlist = "None";
	private static final int ITEM_ID_0 = 0;
	private static final int ITEM_ID_1 = 1;
	private static final int ITEM_ID_2 = 2;
	private static final int ITEM_ID_3 = 3;

	// private int lengthDuringPlaybackPause;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		Log.v("DisplayPlaybackControls: ", "Calling onCreate");
		super.onCreate(savedInstanceState);
		renderPlaybackControls();
		mp.setOnPreparedListener(((MelodyDroidApplication) getApplication())
				.getMDService().getMdsServiceBinder());
		mp.setOnErrorListener(((MelodyDroidApplication) getApplication())
				.getMDService().getMdsServiceBinder());
		mp.setOnCompletionListener(((MelodyDroidApplication) getApplication())
				.getMDService().getMdsServiceBinder());
		//mp.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);

		// TelephonyManager mgr = (TelephonyManager)
		// getSystemService(TELEPHONY_SERVICE);
		// if (mgr != null) {
		// mgr.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
		// }

	}

	private void renderPlaybackControls() {
		setContentView(R.layout.playback);

		screen = getIntent().getStringExtra("screen");
		swipeBackVar = getIntent().getStringExtra("swipeBack");
		position = getIntent().getIntExtra("position", 0);
		playlist = Renderer.playlistName;

		Intent bindIntent = new Intent(this, MDService.class);
		this.bindService(bindIntent, mConnection, Context.BIND_AUTO_CREATE);

		// pb = (ProgressBar) findViewById(R.id.progress_horizontal);
		sb = (SeekBar) findViewById(R.id.your_dialog_seekbar);
		sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				// Message seekValueMsg = new Message();
				// seekValueMsg.arg1 = seekBar.getProgress();
				// mpInterface.getSeekHandler().sendMessage(seekValueMsg);
				try {
				mp.seekTo(seekBar.getProgress() * 1000);
				} catch (IllegalStateException e) {
					Log.e("DisplayPlaybackControls: ", "Media Player is in illegal state.");
				}
			}
		});
		myMarquee = (TextView) findViewById(R.id.myMarquee);
		albumArt = (ImageView) findViewById(R.id.albumArt);
		duration = (TextView) findViewById(R.id.Duration);
		currentTime = (TextView) findViewById(R.id.currentTime);

		((MelodyDroidApplication) getApplication())
				.setDisplayPlaybackControls(this);

		pbll = (LinearLayout) findViewById(R.id.PlaybackLinearLayout);
		pauseImg = new ImageView(this);
		playImg = new ImageView(this);
		skipbImg = new ImageView(this);
		skipfImg = new ImageView(this);
		stopImg = new ImageView(this);

		playImg.setImageResource(R.drawable.menuplay);
		playImg.setMaxWidth(50);
		playImg.setMaxHeight(50);
		playImg.setPadding(15, 0, 50, 0);
		playImg.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mpInterface.resume(mp);
				pbll.removeAllViews();
				pbll.addView(pauseImg);
				pbll.addView(skipbImg);
				pbll.addView(skipfImg);
				pbll.addView(stopImg);
				playPause = "pause";
			}
		});

		pauseImg.setImageResource(R.drawable.menupause);
		pauseImg.setMaxWidth(50);
		pauseImg.setMaxHeight(50);
		pauseImg.setPadding(15, 0, 50, 0);
		pbll.addView(pauseImg);
		pauseImg.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mpInterface.pause(mp);
				pbll.removeAllViews();
				pbll.addView(playImg);
				pbll.addView(skipbImg);
				pbll.addView(skipfImg);
				pbll.addView(stopImg);
				playPause = "play";
			}
		});

		skipbImg.setImageResource(R.drawable.menuskipb);
		skipbImg.setMaxWidth(50);
		skipbImg.setMaxHeight(50);
		skipbImg.setPadding(0, 0, 50, 0);
		pbll.addView(skipbImg);
		skipbImg.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mpInterface.skipBack(mp);
			}
		});

		skipfImg.setImageResource(R.drawable.menuskipf);
		skipfImg.setMaxWidth(50);
		skipfImg.setMaxHeight(50);
		skipfImg.setPadding(0, 0, 50, 0);
		pbll.addView(skipfImg);
		skipfImg.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mpInterface.skipForward(mp);
			}
		});

		// Gesture detection
		gestureDetector = new GestureDetector(this);
	}

	@Override
	public boolean onTouchEvent(MotionEvent me) {
		return gestureDetector.onTouchEvent(me);
	}

	@Override
	public boolean onDown(MotionEvent e) {
		return true;
	}

	@Override
	public void onLongPress(MotionEvent e) {

	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		return true;
	}

	@Override
	public void onShowPress(MotionEvent e) {
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return true;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		try {
			if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH)
				return false;
			// right to left swipe
			if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE
					&& Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
				if ("DisplaySearchCriteriaNames".equals(swipeBackVar)) {
					Intent displaySearchCriteriaNames = new Intent(
							DisplayPlaybackControls.this,
							DisplaySearchCriteriaNames.class);
					startActivity(displaySearchCriteriaNames);
				}
				if ("DisplaySearchCriteriaValues".equals(swipeBackVar)) {
					Intent displaySearchCriteriaValues = new Intent(
							DisplayPlaybackControls.this,
							DisplaySearchCriteriaValues.class);
					startActivity(displaySearchCriteriaValues);
				}
				if ("DisplayMelodies".equals(swipeBackVar)) {
					Intent displayMelodies = new Intent(
							DisplayPlaybackControls.this, DisplayMelodies.class);
					startActivity(displayMelodies);
				}
				if ("DisplayPlaylistMelodies".equals(swipeBackVar)) {
					Intent displayPlaylistMelodies = new Intent(
							DisplayPlaybackControls.this,
							DisplayPlaylistMelodies.class);
					displayPlaylistMelodies.putExtra("playlistName", playlist);
					startActivity(displayPlaylistMelodies);
				}
				if ("DisplayPlaylists".equals(swipeBackVar)) {
					Intent displayPlaylists = new Intent(
							DisplayPlaybackControls.this,
							DisplayPlaylists.class);
					startActivity(displayPlaylists);
				}

			} else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
					&& Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
			}
		} catch (Exception e) {
			Log.v("DisplayPlaybackControls: ",
					"Exception in onFling()" + e.getMessage());
		}
		return false;
	}

	static Handler guiHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			/* get the value from the Message */
			int progress = msg.arg1;
			// pb.setProgress(progress);
			sb.setProgress(progress);
			int progressMin = progress / 60;
			String progressMinString = "";
			if (progressMin < 10)
				progressMinString = "0" + progressMin;
			else
				progressMinString = "" + progressMin;

			int progressSec = progress % 60;
			String progressSecString = "";
			if (progressSec < 10)
				progressSecString = "0" + progressSec;
			else
				progressSecString = "" + progressSec;

			progressString = progressMinString + ":" + progressSecString + "|";
			currentTime.setText(progressString);
		}
	};

	static Handler pbMaxHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			/* get the value from the Message */
			int pbMax = msg.arg1;
			// pb.setMax(pbMax);
			sb.setMax(pbMax);

			int durationMin = pbMax / 60;
			String durationMinString = "";
			if (durationMin < 10)
				durationMinString = "0" + durationMin;
			else
				durationMinString = "" + durationMin;

			int durationSec = pbMax % 60;
			String durationSecString = "";
			if (durationSec < 10)
				durationSecString = "0" + durationSec;
			else
				durationSecString = "" + durationSec;

			durationString = durationMinString + ":" + durationSecString;
			duration.setText(durationString);
		}
	};

	Handler songInfoHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			/* get the value from the Message */
			Object songInfo = msg.obj;
			SongInfo info = (SongInfo) songInfo;
			myMarquee.setText("Album: " + info.getAlbum() + "   Artist: "
					+ info.getSinger() + "   Genre: " + info.getGenre()
					+ "   Title: " + info.getTitle() + "   Year: "
					+ info.getYear() + "   Playlist: " + playlist);
			myMarquee.setTextColor(Color.WHITE);
			myMarquee.setTextSize(25);
			myMarquee.setSelected(true);

			byte[] albumArtArray = info.getAlbumArt();

			if (albumArtArray != null && albumArtArray.length != 0) {
				// byte[] decodedData = Base64.decode(info.getAlbumArt(),
				// Base64.DEFAULT);
				Bitmap bmp = BitmapFactory.decodeByteArray(albumArtArray, 0,
						info.getAlbumArt().length);
				Log.v("Original bitmap width: ", "" + bmp.getWidth());
				Log.v("Original bitmap height: ", "" + bmp.getHeight());
				Bitmap newBmp = Bitmap.createScaledBitmap(bmp, 200, 200, false);
				albumArt.setImageBitmap(newBmp);
			} else {
				Bitmap bmp = BitmapFactory.decodeResource(getResources(),
						R.drawable.default_album_artwork);
				Bitmap newBmp = Bitmap.createScaledBitmap(bmp, 200, 200, false);
				albumArt.setImageBitmap(newBmp);
			}

		}
	};

	static Handler playPauseHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			/* get the value from the message */
			Object playPauseObj = msg.obj;
			playPause = (String) playPauseObj;

			pbll.removeAllViews();

			if ("play".equals(playPause)) {
				playImg.setImageResource(R.drawable.menuplay);
				playImg.setMaxWidth(50);
				playImg.setMaxHeight(50);
				playImg.setPadding(15, 0, 50, 0);
				pbll.addView(playImg);
				playImg.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						mpInterface.resume(mp);
						pbll.removeAllViews();
						pbll.addView(pauseImg);
						pbll.addView(skipbImg);
						pbll.addView(skipfImg);
						pbll.addView(stopImg);
						playPause = "pause";
					}
				});
			}

			if (playPause == null || "pause".equals(playPause)) {
				pauseImg.setImageResource(R.drawable.menupause);
				pauseImg.setMaxWidth(50);
				pauseImg.setMaxHeight(50);
				pauseImg.setPadding(15, 0, 50, 0);
				pbll.addView(pauseImg);
				pauseImg.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						mpInterface.pause(mp);
						pbll.removeAllViews();
						pbll.addView(playImg);
						pbll.addView(skipbImg);
						pbll.addView(skipfImg);
						pbll.addView(stopImg);
						playPause = "play";
					}
				});
			}

			skipbImg.setImageResource(R.drawable.menuskipb);
			skipbImg.setMaxWidth(50);
			skipbImg.setMaxHeight(50);
			skipbImg.setPadding(0, 0, 50, 0);
			pbll.addView(skipbImg);
			skipbImg.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					mpInterface.skipBack(mp);
				}
			});

			skipfImg.setImageResource(R.drawable.menuskipf);
			skipfImg.setMaxWidth(50);
			skipfImg.setMaxHeight(50);
			skipfImg.setPadding(0, 0, 50, 0);
			pbll.addView(skipfImg);
			skipfImg.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					mpInterface.skipForward(mp);
				}
			});

			
			removeMessages(msg.what); 
		};
	};

	public Handler getPlayPauseHandler() {
		return playPauseHandler;
	}

	public void setPlayPauseHandler(Handler playPauseHandler) {
		DisplayPlaybackControls.playPauseHandler = playPauseHandler;
	}

	public Handler getSongInfoHandler() {
		return songInfoHandler;
	}

	public void setSongInfoHandler(Handler songInfoHandler) {
		this.songInfoHandler = songInfoHandler;
	}

	public Handler getPbMaxHandler() {
		return pbMaxHandler;
	}

	public void setPbMaxHandler(Handler pbMaxHandler) {
		DisplayPlaybackControls.pbMaxHandler = pbMaxHandler;
	}

	public Handler getGuiHandler() {
		return guiHandler;
	}

	public void setGuiHandler(Handler guiHandler) {
		DisplayPlaybackControls.guiHandler = guiHandler;
	}

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			Log.v("DisplayPlaybackControls: ", "Calling onServiceConnected");
			mpInterface = (MDSInterface) service;
			Renderer.renderDisplayControl = true;
			if ("DisplayMelodies".equals(screen)
					|| "DisplayPlaylistMelodies".equals(screen)
					|| "MelodySearchActivity".equals(screen)) {
				getMp().reset();
				mpInterface.playFile(position, mp);
			}

			if ("MDService".equals(screen)) {
				// if (!DisplayPlaybackControls.getMp().isPlaying())
				// mpInterface.playFile(position, mp);
			}

			mpInterface.getMessages(mp);
		}

		public void onServiceDisconnected(ComponentName className) {
			mpInterface = null;

		}
	};

	// public boolean onKeyDown(int keyCode, KeyEvent event) {
	// if (keyCode == KeyEvent.KEYCODE_BACK) {
	// // put your activity into background and restore the previous
	// // activity from the stack
	// return this.moveTaskToBack(true);
	// } else
	// return super.onKeyDown(keyCode, event);
	// }

	@Override
	protected void onNewIntent(Intent intent) {
		Log.v("DisplayPlaybackControls: ", "Calling onNewIntent");
		screen = intent.getStringExtra("screen");
		position = intent.getIntExtra("position", 0);
		playlist = Renderer.playlistName;

		this.bindService(new Intent(this, MDService.class), mConnection,
				Context.BIND_AUTO_CREATE);

		if ("service".equals(screen)) {
			playPause = intent.getStringExtra("playPause");
		}
		if (!"service".equals(screen)) {
			swipeBackVar = intent.getStringExtra("swipeBack");
		}

		// pb = (ProgressBar) findViewById(R.id.progress_horizontal);
		// sb = (SeekBar) findViewById(R.id.your_dialog_seekbar);
		// sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
		//
		// @Override
		// public void onProgressChanged(SeekBar seekBar, int progress,
		// boolean fromUser) {
		// // TODO Auto-generated method stub
		// }
		//
		// @Override
		// public void onStartTrackingTouch(SeekBar seekBar) {
		// // TODO Auto-generated method stub
		// }
		//
		// @Override
		// public void onStopTrackingTouch(SeekBar seekBar) {
		// // TODO Auto-generated method stub
		// Message seekValueMsg = new Message();
		// seekValueMsg.arg1 = seekBar.getProgress();
		// // Toast toast = Toast.makeText(((MelodyDroidApplication)
		// // getApplication()).getDisplayPlaybackControls(), "progress: "
		// // + seekBar.getProgress(), Toast.LENGTH_SHORT);
		// // toast.show();
		// mpInterface.getSeekHandler().sendMessage(seekValueMsg);
		// }
		// });
		// myMarquee = (TextView) findViewById(R.id.myMarquee);
		// albumArt = (ImageView) findViewById(R.id.albumArt);
		((MelodyDroidApplication) getApplication())
				.setDisplayPlaybackControls(this);

	}

	@Override
	protected void onPause() {
		Log.v("DisplayPlaybackControls: ", "Calling onPause");
		super.onPause();
		screen = null;
		unbindService(mConnection);
	}

	@Override
	protected void onResume() {
		Log.v("DisplayPlaybackControls: ", "Calling onResume");
		super.onResume();
		this.bindService(new Intent(this, MDService.class), mConnection,
				Context.BIND_AUTO_CREATE);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// ignore orientation change
		super.onConfigurationChanged(newConfig);
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, ITEM_ID_0, 0, R.string.home).setIcon(R.drawable.melodica);
		menu.add(0, ITEM_ID_1, 1, R.string.search).setIcon(
				R.drawable.ic_menu_search);
		menu.add(0, ITEM_ID_2, 2, R.string.play_list).setIcon(
				R.drawable.playlist);
		menu.add(0, ITEM_ID_3, 3, R.string.dbsync).setIcon(R.drawable.syncicon);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case ITEM_ID_0:
			SearchCriteria.clearAll();
			Intent displaySearchCriteriaNames = new Intent(this,
					DisplaySearchCriteriaNames.class);
			startActivity(displaySearchCriteriaNames);
			return true;
		case ITEM_ID_1:
			onSearchRequested();
			return true;
		case ITEM_ID_2:
			Intent displayPlaylists = new Intent(this, DisplayPlaylists.class);
			startActivity(displayPlaylists);
			return true;
		case ITEM_ID_3:
			DatabaseSynchronizationService dbSyncService = DatabaseSynchronizationService
					.getInstance(DisplayPlaybackControls.this);
			dbSyncService.findMelodiesFromSourceAndSynchDatabase();
			return true;
		}

		return false; // should never happen
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// mp.stop();
		// mp.release();

		// TelephonyManager mgr = (TelephonyManager)
		// getSystemService(TELEPHONY_SERVICE);
		// if (mgr != null) {
		// mgr.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
		// }

	}

	// PhoneStateListener phoneStateListener = new PhoneStateListener() {
	// @Override
	// public void onCallStateChanged(int state, String incomingNumber) {
	// if (state == TelephonyManager.CALL_STATE_RINGING) {
	// mp.pause();
	// lengthDuringPlaybackPause = mp.getCurrentPosition();
	// } else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
	// lengthDuringPlaybackPause = mp.getCurrentPosition();
	// mp.pause();
	// } else if (state == TelephonyManager.CALL_STATE_IDLE) {
	// mp.seekTo(lengthDuringPlaybackPause);
	// mp.start();
	// mpInterface.getMessages(mp);
	// }
	// super.onCallStateChanged(state, incomingNumber);
	// }
	//
	// };

	public static MediaPlayer getMp() {
		return mp;
	}

	public static void setMp(MediaPlayer mp) {
		DisplayPlaybackControls.mp = mp;
	}

}
