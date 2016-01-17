package com.droid.melodydroid.core;

import android.media.MediaPlayer;
import android.os.Handler;

public interface MDSInterface {

	void clearPlaylist();

	void addSongPlaylist(String song);

	void playFile(int position, MediaPlayer mpInner);

	void pause(MediaPlayer mpInner);

	void resume(MediaPlayer mpInner);

	void skipForward(MediaPlayer mpInner);

	void skipBack(MediaPlayer mpInner);

	void getMessages(MediaPlayer mpInner);

	// Handler getSeekHandler();
	//	void setSeekHandler(Handler seekHandler);
}
