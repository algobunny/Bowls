package me.valour.bowls;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.TextView;

public class BowlView extends TextView {
	
	private Paint primaryPaint;
	private Paint textPaint;
	private int radius;
	public User user;
	
	public BowlView(Context context, AttributeSet ats, int ds){
		super(context, ats, ds);
	}
	
	public BowlView (Context context) {
	    super(context);
	  }

	 public BowlView(Context context, AttributeSet attr) {
	    super(context, attr);
	  }
	 
	 @Override
	 public void setId(int id){
		 super.setId(id);
		 user = new User(id);
	 }
	 
	 public void init(int color, int radius){
		 primaryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		 primaryPaint.setColor(color);
		 textPaint = new Paint(Paint.LINEAR_TEXT_FLAG);
		 textPaint.setColor(Color.WHITE);
		 textPaint.setTextSize((float)20.5);
		 this.setTextColor(Color.WHITE);
		 this.radius = radius;
		 this.setMaxWidth(radius);
		 this.setMaxHeight(radius);
		 this.setMinWidth(radius);
		 this.setMinHeight(radius);
	 }
	 
	 @Override
	  public void onDraw(Canvas canvas) {
		 canvas.drawCircle(0, 0, radius, primaryPaint);
		 String p =String.format("$ %.2f", user.getTotal());
		 canvas.drawText(p, -1*radius, 0, textPaint);
		// super.onDraw(canvas);
	 }
	 
	 
}
