package me.valour.bowls;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.R.color;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

public class BowlsGroup extends FrameLayout {

	int mMeasuredWidth;
	int mMeasuredHeight;
	int centerX;
	int centerY;
	int tableRadius;
	int bowlRadius;
	boolean measuredScreen = false;
	boolean selectReady = false;
	
	FrameLayout.LayoutParams defaultParams;
	BowlSelectListener bowlSelect;

	ArrayList<BowlView> bowls;
	int bowlsIdCounter = 1;

	public BowlsGroup(Context context) {
		super(context);
		init();
	}

	public BowlsGroup(Context context, AttributeSet ats, int ds) {
		super(context, ats, ds);
		init();
	}

	public BowlsGroup(Context context, AttributeSet attr) {
		super(context, attr);
		init();
	}
	
	@Override
	protected void onLayout (boolean changed, int left, int top, int right, int bottom){
		measureView();	
		
		double angleDelta = Math.PI*2.0/bowls.size();
		double topX = 0;
		double topY = -1.0*tableRadius;
		
		int i= 0;
		 for(BowlView bowl: bowls){
			bowl.setRadius(bowlRadius); 
			bowl.bringToFront();
			double angle = angleDelta*i;
			double px = Math.cos(angle)*topX - Math.sin(angle)*topY + centerX;
			double py = Math.sin(angle)*topX - Math.cos(angle)*topY + centerY;
			bowl.setX((float)px);
			bowl.setY((float)py);
			Log.d("vars",String.format("x=%f \t y=%f",px, py));
			i++;
		 }
		 super.onLayout(changed, left, top, right, bottom);
	}

	private void init() {
		bowlSelect = new BowlSelectListener();
		defaultParams = new FrameLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		setClickable(true);
		setFocusable(true);

		bowls = new ArrayList<BowlView>();
		for (int i = 1; i <= Kitchen.minBowls; i++) {
			BowlView bowl = new BowlView(this.getContext());
			bowl.setId(bowlsIdCounter);
			bowl.setColors(Kitchen.assignColor(i+1));
			bowls.add(bowl);
			bowlsIdCounter++;
			this.addView(bowl, defaultParams);
			bowl.setOnTouchListener(bowlSelect);
		}
		
	}
	
	
	public void measureView(){
		if(measuredScreen){
			return;
		}
			mMeasuredWidth = getMeasuredWidth();
			mMeasuredHeight = getMeasuredHeight();		
			centerX = mMeasuredWidth / 2;
			centerY = mMeasuredHeight / 2;		
			tableRadius = Math.min(centerX, centerY);
			
			double q = ((double) tableRadius * 2.0 * Math.PI)
					/ (double) Kitchen.maxBowls;
			bowlRadius = (int) (q / 2.0);
			tableRadius -= bowlRadius;
			Log.d("vars", String.format("bowl radius=%d", bowlRadius));
			Log.d("vars", String.format("table radius=%d", tableRadius));
			
			measuredScreen = true;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		int measuredWidth = measure(widthMeasureSpec);
		int measuredHeight = measure(heightMeasureSpec);
		int d = Math.min(measuredWidth, measuredHeight);
		setMeasuredDimension(d, d);

	}

	private int measure(int measureSpec) {
		int result = 0;
		int specMode = MeasureSpec.getMode(measureSpec);
		int specSize = MeasureSpec.getSize(measureSpec);

		if (specMode == MeasureSpec.UNSPECIFIED) {
			result = 500;
		} else {
			result = specSize;
		}
		return result;
	}

	public BowlView addBowl() {
		int i = bowls.size() + 1;
		BowlView bowl = new BowlView(this.getContext());
		bowl.setId(bowlsIdCounter);
		bowl.setColors(Kitchen.assignColor(i));
		bowl.setX(centerX);
		bowl.setY(centerY);
		bowls.add(bowl);
		addView(bowl, defaultParams);
		bowl.setOnTouchListener(bowlSelect);
		bowlsIdCounter++;
		return bowl;
	}

	public void refreshBowls() {
		 for(BowlView bv: bowls){ 
			bv.formatText(); 
			 bv.invalidate();
		 }
	}

	public List<Integer> getBowlViewIds() {
		ArrayList<Integer> ids = new ArrayList<Integer>();
		for (BowlView bw : bowls) {
			ids.add(bw.getId());
		}
		return ids;
	}

	public List<User> getBowlUsers() {
		ArrayList<User> users = new ArrayList<User>();
		for (BowlView bw : bowls) {
			users.add(bw.user);
		}
		return users;
	}

	public void bowlsFocus(boolean unfade) {
		for (BowlView bv : bowls) {
			if (unfade) {
				bv.unfade();
			} else {
				bv.fade();
			}
		}
	}
	
	
	public void clearSelection(){
		bowlSelect.selected.clear();
		for(BowlView bv: bowls){
			bv.setSelected(false);
		}
	}
	
	public void readyBowlSelect(){
		selectReady = true;
		clearSelection();
		bowlsFocus(false);
	}
	
	public void stopBowlSelect(){
		selectReady = false;
		clearSelection();
		bowlsFocus(true);
	}
	
	public List<User> getSelectedUsers(){
		return bowlSelect.selected;
	}
	
	private class BowlSelectListener implements OnTouchListener{

		public List<User> selected;
		
		public BowlSelectListener(){
			selected = new ArrayList<User>();
		}
		
		@Override
		public boolean onTouch(View v, MotionEvent move) {
			BowlView bv = (BowlView)v;
			int action = move.getAction();
			if(selectReady && action==MotionEvent.ACTION_DOWN){
				if(bv.toggleSelected()){
					selected.remove(bv.user);
				} else {
					selected.add(bv.user);
				}
				Log.d("vars", bv.getId()+" touched down");
			}
			return false;
		}
		
	}

}