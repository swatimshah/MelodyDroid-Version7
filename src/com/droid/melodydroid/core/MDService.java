package com.droid.melodydroid.core;

import java.util.ArrayList;
import java.util.List;

import com.droid.melodydroid.R;
import com.droid.melodydroid.data.DataHelper;
import com.droid.melodydroid.display.DisplayPlaybackControls;
import com.droid.melodydroid.display.SongInfo;
import com.droid.melodydroid.helper.MelodyDroidHelper;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

public class MDService extends Service {

	private int songDuration = 0;
	private List<String> songs = new ArrayList<String>();
	private int currentPosition;
	private NotificationManager nm;
	private static final int NOTIFY_ID = 100;
	private static final int FOREGROUND_SERVICE_ID = 1000;
	private MDServiceBinder mdsServiceBinder = new MDServiceBinder();
	public Handler pbHandler;
	MelodyDroidApplication mda;
	Message pbMaxMsg = new Message();
	Message songInfo = new Message();
	Message playPauseMsg = new Message();
	Message progressMsg = new Message();
	String currentlyPlayedFile = "";
	int currentPositionOfProgressBar;
	private int lengthDuringPlaybackPause;
	private boolean mpWasPlayingWhenPhoneRang = false;

	@Override
	public void onCreate() {
		super.onCreate();
		mda = (MelodyDroidApplication) getApplication();
		mda.setMDService(this);
		nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		TelephonyManager mgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		if (mgr != null) {
			mgr.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
		}

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		nm.cancel(NOTIFY_ID);

		TelephonyManager mgr = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		if (mgr != null) {
			mgr.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
		}

	}

	@Override
	public IBinder onBind(Intent arg0) {
		return mdsServiceBinder;
	}

	private class MDServiceBinder extends Binder implements MDSInterface,
			MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
			MediaPlayer.OnCompletionListener {

		public void playFile(int position, MediaPlayer mpInner) {
			try {
				Log.v("MDService: The size of songs list: ", "" + songs.size());
				if (MelodyDroidHelper.isSdPresent()) {
					currentPosition = position;
					playSong(songs.get(position), false, mpInner);
				} else {
					Toast toast = Toast.makeText(MDService.this,
							"SD Card is not available", Toast.LENGTH_SHORT);
					toast.show();
				}
			} catch (IndexOutOfBoundsException e) {
				Log.v("MDService: ", e.getMessage());
			}

		}

		public void onCompletion(MediaPlayer arg0) {
			onCompletionTasks(arg0);
		}

		public void onPrepared(final MediaPlayer mpInner) {

			Log.v("MDService: ", "In onPreparedMethod");

			mpInner.start();

			/* Creating a message */
			pbMaxMsg = new Message();
			songDuration = mpInner.getDuration();
			pbMaxMsg.arg1 = songDuration / 1000;
			/* Sending the message */
			mda.getDisplayPlaybackControls().getPbMaxHandler()
					.sendMessage(pbMaxMsg);

			/* Creating a message */
			songInfo = new Message();
			try {
				songInfo.obj = getSongInfo(currentlyPlayedFile);
			} catch (SQLiteException sqle) {
				SongInfo myDummySongInfo = new SongInfo();
				myDummySongInfo.setAlbum("");
				myDummySongInfo.setFileName("");
				myDummySongInfo.setGenre("");
				myDummySongInfo.setSinger("");
				myDummySongInfo.setTitle("");
				myDummySongInfo.setYear("");
				songInfo.obj = myDummySongInfo;
				Log.v("MDService: ", sqle.getMessage());
				Context context = getApplicationContext();
				CharSequence text = "Error playing the song "
						+ currentlyPlayedFile;
				int duration = Toast.LENGTH_SHORT;

				Toast toast = Toast.makeText(context, text, duration);
				toast.show();
			}
			/* Sending the message */
			mda.getDisplayPlaybackControls().getSongInfoHandler()
					.sendMessage(songInfo);

			/* Creating a message */
			playPauseMsg = new Message();
			playPauseMsg.obj = "pause";
			/* Sending the message */
			mda.getDisplayPlaybackControls().getPlayPauseHandler()
					.sendMessage(playPauseMsg);

			new Thread(new Runnable() {
				@Override
				public void run() {
					int previousPosition = -1;
					int nextPosition = 0;
					try {
						nextPosition = mpInner.getCurrentPosition();
					} catch (Exception e) {
						Log.e("MDService: ", "Mediaplayer is in invalid state.");
					}
					while (previousPosition != nextPosition) {

						/* Creating a message */
						Message progressMsgInThread = new Message();
						progressMsg = progressMsgInThread;
						progressMsgInThread.arg1 = nextPosition / 1000;
						/* Sending the message */
						if (mda.getDisplayPlaybackControls() != null)
							mda.getDisplayPlaybackControls().getGuiHandler()
									.sendMessage(progressMsgInThread);
						;
						currentPositionOfProgressBar = nextPosition / 1000;
						previousPosition = nextPosition;

						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						try {
							nextPosition = mpInner.getCurrentPosition();
						} catch (Exception e) {
							Log.e("MDService: ",
									"Mediaplayer is in invalid state.");
						}

					}
				}
			}).start();

		}

		// Handler seekHandler = new Handler() {
		// @Override
		// public void handleMessage(Message msg) {
		// super.handleMessage(msg);
		// mp.seekTo(msg.arg1 * 1000);
		// }
		// };
		//
		// public Handler getSeekHandler() {
		// return seekHandler;
		// }
		//
		// public void setSeekHandler(Handler seekHandler) {
		// this.seekHandler = seekHandler;
		// }

		public void addSongPlaylist(String song) {
			songs.add(song);
		}

		public void clearPlaylist() {
			songs.clear();
		}

		public void skipBack(MediaPlayer mpInner) {
			prevSong(mpInner);
		}

		public void skipForward(MediaPlayer mpInner) {
			nextSong(mpInner);
		}

		public void pause(MediaPlayer mpInner) {
			nm.cancel(NOTIFY_ID);
			try {
				if (mpInner.isPlaying())
					mpInner.pause();
			} catch (Exception e) {
				Log.e("MDService: ", "MediaPlayer is in illegal state");
			}
		}

		public void resume(MediaPlayer mpInner) {
			try {
				playSong(songs.get(currentPosition), true, mpInner);
			} catch (IndexOutOfBoundsException e) {
				Log.v("MDService: ", e.getMessage());
			}

		}

		@Override
		public void getMessages(final MediaPlayer mpInner) {
			/* Creating a message */
			pbMaxMsg = new Message();
			pbMaxMsg.arg1 = songDuration / 1000;
			/* Sending the message */
			if (mda.getDisplayPlaybackControls() != null)
				mda.getDisplayPlaybackControls().getPbMaxHandler()
						.sendMessage(pbMaxMsg);
			/* Creating a message */
			songInfo = new Message();
			try {
				songInfo.obj = getSongInfo(currentlyPlayedFile);
			} catch (SQLiteException sqle) {
				SongInfo myDummySongInfo = new SongInfo();
				myDummySongInfo.setAlbum("");
				myDummySongInfo.setFileName("");
				myDummySongInfo.setGenre("");
				myDummySongInfo.setSinger("");
				myDummySongInfo.setTitle("");
				myDummySongInfo.setYear("");
				songInfo.obj = myDummySongInfo;
				Log.v("MDService: ", sqle.getMessage());
				Context context = getApplicationContext();
				CharSequence text = "Error playing the song "
						+ currentlyPlayedFile;
				int duration = Toast.LENGTH_SHORT;

				Toast toast = Toast.makeText(context, text, duration);
				toast.show();
			}
			/* Sending the message */
			if (mda.getDisplayPlaybackControls() != null)
				mda.getDisplayPlaybackControls().getSongInfoHandler()
						.sendMessage(songInfo);
			/* Creating a message */
			try {
				if (mpInner.isPlaying()) {
					playPauseMsg = new Message();
					playPauseMsg.obj = "pause";
				} else {
					playPauseMsg = new Message();
					playPauseMsg.obj = "play";
				}
			} catch (Exception e) {
				Log.e("MDService: ",
						"Media Player is in illegal state for play pause.");
				playPauseMsg.obj = null;
			}
			/* Sending the message */
			if (mda.getDisplayPlaybackControls() != null
					&& playPauseMsg.obj != null) {

				mda.getDisplayPlaybackControls().getPlayPauseHandler()
						.sendMessage(playPauseMsg);
			}

			new Thread(new Runnable() {
				@Override
				public void run() {
					int previousPosition = -1;
					int nextPosition = 0;
					try {
						nextPosition = mpInner.getCurrentPosition();
					} catch (IllegalStateException e) {
						Log.e("MDService: ",
								"Media Player is in illegal state for calculating next position in the beginning.");
					}
					while (previousPosition != nextPosition) {

						/* Creating a message */
						Message progressMsgInGetMsg = new Message();
						progressMsg = progressMsgInGetMsg;
						progressMsgInGetMsg.arg1 = nextPosition / 1000;
						/* Sending the message */
						if (mda.getDisplayPlaybackControls() != null)
							mda.getDisplayPlaybackControls().getGuiHandler()
									.sendMessage(progressMsgInGetMsg);
						;
						currentPositionOfProgressBar = nextPosition / 1000;
						previousPosition = nextPosition;

						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}

						try {
							nextPosition = mpInner.getCurrentPosition();
						} catch (IllegalStateException e) {
							Log.e("MDService: ",
									"Media Player is in illegal state for calculating next position.");
						}
					}
				}
			}).start();

		}

		@Override
		public boolean onError(MediaPlayer mp, int what, int extra) {
			// TODO Auto-generated method stub

			Log.v("MDService: ", "what: " + Integer.toString(what));
			Log.v("MDService: ", "extra: " + Integer.toString(extra));
			if (songs.size() > 0) {
				mp.reset();
				playSongOnError(songs.get(currentPosition), false, mp);
			}

			return true;
		}

	};

	private void playSong(String file, boolean resume, MediaPlayer mpInner) {

		currentlyPlayedFile = file;

		try {
			displayNotificationPlayback(nm, file);
			if (!resume) {
				Log.v("MDService: ", "Playing song");
				// while (mpInner.isPlaying())
				// ;

				Log.v("MDService: Currently playing ", "" + file);
				mpInner.setDataSource(file);
				Log.v("MDService: setDataSource ", "" + file);
				mpInner.prepareAsync();
				Log.v("MDService: prepareAsync ", "" + file);

				// mpInner.setOnPreparedListener(mdsServiceBinder);
				// mpInner.setOnErrorListener(mdsServiceBinder);
			} else {
				Log.v("MDService: ", "Resuming song");
				mpInner.start();
				mdsServiceBinder.getMessages(mpInner);
			}

		} catch (Exception e) {
			Log.v("MDService: ", "Media player is in illegal state.");
			Context context = getApplicationContext();
			CharSequence text = "Error playing the song " + file;
			int duration = Toast.LENGTH_SHORT;

			Toast toast = Toast.makeText(context, text, duration);
			toast.show();
		}
	}

	private void playSongOnError(String file, boolean resume,
			MediaPlayer mpInner) {

		currentlyPlayedFile = file;

		try {
			displayNotificationPlayback(nm, file);
			if (!resume) {
				Log.v("MDService: ", "Playing song");
				// while (mpInner.isPlaying())
				// ;

				Log.v("MDService: Currently playing ", "" + file);
				mpInner.setDataSource(file);
				Log.v("MDService: setDataSource ", "" + file);
				mpInner.prepare();
				Log.v("MDService: prepare ", "" + file);
				//mdsServiceBinder.onPrepared(mpInner);
				mpInner.start();
				// mpInner.setOnPreparedListener(mdsServiceBinder);
				// mpInner.setOnErrorListener(mdsServiceBinder);
			} else {
				Log.v("MDService: ", "Resuming song");
				mpInner.start();
				mdsServiceBinder.getMessages(mpInner);
			}

		} catch (Exception e) {
			Log.v("MDService: ", "Media player is in illegal state.");
			Context context = getApplicationContext();
			CharSequence text = "Error playing the song " + file;
			int duration = Toast.LENGTH_SHORT;

			Toast toast = Toast.makeText(context, text, duration);
			toast.show();
		}
	}

	private void onCompletionTasks(MediaPlayer mpInner) {
		// Check if last song or not
		if (++currentPosition >= songs.size()) {
			--currentPosition;
			nm.cancel(NOTIFY_ID);
			// DisplayPlaybackControls.getMp().release();
			mpInner.reset();
			/* Creating a message */
			Message playPauseMsg = new Message();
			playPauseMsg.obj = "play";
			/* Sending the message */
			mda.getDisplayPlaybackControls().getPlayPauseHandler()
					.sendMessage(playPauseMsg);

		} else {
			mpInner.reset();
			playSong(songs.get(currentPosition), false, mpInner);
		}
	}

	private Object getSongInfo(String file) {
		DataHelper dataHelper = new DataHelper(this);
		SongInfo songInfo = dataHelper.getSongInfo(file);
		return songInfo;
	}

	private NotificationManager displayNotificationPlayback(
			NotificationManager nm, String fileName) {
		Notification notification = new Notification(R.drawable.playbackstart,
				"Playing Melody", System.currentTimeMillis());
		Context context = getApplicationContext();
		Intent intent = new Intent(this, DisplayPlaybackControls.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtra("screen", "MDservice");
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				intent, PendingIntent.FLAG_UPDATE_CURRENT);
		notification.flags = Notification.FLAG_ONGOING_EVENT;
		notification.setLatestEventInfo(context, "Currently Playing", fileName,
				contentIntent);
		nm.notify(NOTIFY_ID, notification);
		return nm;
	}

	private void nextSong(MediaPlayer mpInner) {
		// Check if last song or not
		if (++currentPosition >= songs.size()) {
			currentPosition = 0;
			nm.cancel(NOTIFY_ID);
			if (songs.size() > 0) {
				mpInner.reset();
				playSong(songs.get(currentPosition), false, mpInner);
			}
		} else {
			if (songs.size() > 0) {
				mpInner.reset();
				playSong(songs.get(currentPosition), false, mpInner);
			}
		}
	}

	private void prevSong(MediaPlayer mpInner) {
		if (currentPosition >= 1) {
			mpInner.reset();
			playSong(songs.get(--currentPosition), false, mpInner);
		} else {
			if (songs.size() > 0) {
				mpInner.reset();
				playSong(songs.get(currentPosition), false, mpInner);
			}
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		// TODO Auto-generated method stub

		Log.e("MDService: ", "Starting MDService onStartCommand...");
		showStartForeGroundNotification();
		return START_STICKY;

	}

	private void showStartForeGroundNotification() {
		// TODO Auto-generated method stub
		Intent notificationIntent = new Intent(this,
				DisplayPlaybackControls.class);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		notificationIntent.putExtra("screen", "MDService");
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		Notification notification = new Notification.Builder(this)
				.setContentTitle("MelodyDroid").setTicker("MelodyDroid")
				.setContentText("Melodies' Collection")
				.setSmallIcon(R.drawable.melodica_notification)
				.setContentIntent(pendingIntent).setOngoing(true).build();
		
		startForeground(FOREGROUND_SERVICE_ID, notification);

	}

	PhoneStateListener phoneStateListener = new PhoneStateListener() {
		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			if (state == TelephonyManager.CALL_STATE_RINGING) {
				if (DisplayPlaybackControls.getMp().isPlaying()) {
					mpWasPlayingWhenPhoneRang = true;
					DisplayPlaybackControls.getMp().pause();
					lengthDuringPlaybackPause = DisplayPlaybackControls.getMp()
							.getCurrentPosition();
				}
			} else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
				if (DisplayPlaybackControls.getMp().isPlaying()) {
					mpWasPlayingWhenPhoneRang = true;
					lengthDuringPlaybackPause = DisplayPlaybackControls.getMp()
							.getCurrentPosition();
					DisplayPlaybackControls.getMp().pause();
				}
			} else if (state == TelephonyManager.CALL_STATE_IDLE) {
				if (mpWasPlayingWhenPhoneRang) {
					DisplayPlaybackControls.getMp().seekTo(
							lengthDuringPlaybackPause);
					DisplayPlaybackControls.getMp().start();
					mdsServiceBinder.getMessages(DisplayPlaybackControls
							.getMp());
					mpWasPlayingWhenPhoneRang = false;
				}
			}
			super.onCallStateChanged(state, incomingNumber);
		}

	};

	public MDServiceBinder getMdsServiceBinder() {
		return mdsServiceBinder;
	}

	public void setMdsServiceBinder(MDServiceBinder mdsServiceBinder) {
		this.mdsServiceBinder = mdsServiceBinder;
	}

}
