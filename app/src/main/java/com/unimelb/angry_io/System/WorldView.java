package com.unimelb.angry_io.System;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import com.unimelb.angry_io.Entity.EntityManager;
import com.unimelb.angry_io.Misc.ScoreBoard;
import com.unimelb.angry_io.Network.BluetoothService;
import com.unimelb.angry_io.Pontential.GameMap;
import com.unimelb.angry_io.Pontential.PlayerUnit;
import com.unimelb.angry_io.R;


public class WorldView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    // tags; used for log
    private final String TAG="WorldView";

    private Context mGameContext;
	private SurfaceHolder surfaceHolder;
	private boolean running = false;

    //gameMap info
    public GameMap mGameMap;
    Point GameMapPoint = new Point(0, 0);
    public static float GameMapX = 0;
    public static float GameMapY = 0;

    //background image
    private PlayerUnit mPlayerUnit = null;
    private Bitmap mBackgroundImage = null;
    private Bitmap mBackgroundImage_day = null;
    private Bitmap mBackgroundImage_night =null;

	// Used for touch event
	private boolean touched = false;
	private float touched_x, touched_y;
    private float release_x, release_y;

    //Used for split icon touch event
    public static boolean split_icon_touched = false;
    public static boolean hasSplit = false;

    // Constant paints for split icon in down left corner
    private Paint split_icon_1=new Paint();
    private Paint split_icon_2=new Paint();

    // scoreBoard info
    private static boolean score_icon_touched = false;
    private ScoreBoard scoreBoard;

	// EntityManager used for manage all the entities
	public EntityManager entityManager = null;

    // screen's width and height
    private int width;
    private int height;
    // background png's width and height
    public static int png_width;
    public static int png_height;

    private WindowManager windowManager;
    private Display display;

    private Paint StutasTextPaint = null;

    Paint background_paint = new Paint();

    // Game mode info
    private static final int GAME_SINGOLE_MODE = 1;
    private static final int GAME_MULTI_MODE = 2;
    private int GAME_MODE = GAME_MULTI_MODE;
    private BluetoothService bluetoothService;

    float sensorX = 0;
    float sensorY = 0;
    //For scheme
    public static final int DAY_MODE = 0;
    public static final int NIGHT_MODE = 1;

    private String strScheme = "";

	public WorldView(Context context, AttributeSet attrs) {
		super(context, attrs);
        mGameContext = context;
		Sensors aSensor = new Sensors(this, context);
		getHolder().addCallback(this);
		setFocusable(true);
        windowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        display = windowManager.getDefaultDisplay();
        bluetoothService = ((AngryResource) context.getApplicationContext()).getBTService();
        if(bluetoothService == null){
            Log.e("Randy", "!!!!!!!!!Single mode \n");
            GAME_MODE = GAME_SINGOLE_MODE;
        }

        scoreBoard = new ScoreBoard(this);
	}
	
	@SuppressLint("WrongCall")
	public void run() {
		// initialize a circle to indicate the touch event
		Paint touchPoint = new Paint();
		touchPoint.setAntiAlias(true);
		touchPoint.setColor(Color.GREEN);

		while(running) {
			Canvas canvas = null;
			try {
				canvas = surfaceHolder.lockCanvas(null);
				synchronized(surfaceHolder) {
					onDraw(canvas);
                    // if there is a touch event,reset the direction of ball
					if(touched){
						//Log.d(TAG, "dectect touch event");
						canvas.drawCircle(touched_x,touched_y,20,touchPoint);
						entityManager.touchEvent(touched_x, touched_y);
					}else{
                        entityManager.untouchEvent();
                    }
                    entityManager.update();
                    entityManager.onDraw(canvas);
                    if(split_icon_touched){
                        //Log.d(TAG, "dectect split icon touched");
                        onDrawSplitIcon(canvas, Color.MAGENTA, Color.CYAN);
                        //entityManager.untouchEvent();
                        entityManager.splitIconEvent();
                    }else {
                        onDrawSplitIcon(canvas, Color.CYAN, Color.MAGENTA);
                    }
                    // TODO fill the score_icon
                    if(score_icon_touched){
                        scoreBoard.get_one_press();
                        score_icon_touched = false;
                    }
                    scoreBoard.onDraw(canvas);
				}
        	} finally {
        		if (canvas != null) {
        			surfaceHolder.unlockCanvasAndPost(canvas);
        		}
        	}
			try {
				//Thread.sleep(10);
			} catch(Exception e) {}
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {

	}

	@Override
	public void surfaceCreated(SurfaceHolder surfaceHolder) {
		this.surfaceHolder = surfaceHolder;
		this.running = true;

        setupGameMap();

		// TODO integrate map& AI balls to EntityManager
        setPlayerStart();

        Resources res = mGameContext.getResources();
        try {
            mBackgroundImage_day = BitmapFactory.decodeResource(res, R.drawable.grid_bg);
            mBackgroundImage_night = BitmapFactory.decodeResource(res, R.drawable.grid_bg_n);
        } catch(OutOfMemoryError oe){

        }
        mBackgroundImage = mBackgroundImage_day;
        //get the png height and width
        png_height = mBackgroundImage.getHeight();
        png_width = mBackgroundImage.getWidth();

        // set the background color as white
        background_paint.setColor(Color.WHITE);

        //get the screen height and width to initialize EntityManager
        width = getWidth();
        height = getHeight();

        entityManager=new EntityManager(this,width,height);
        // Add this entityManager to the scoreboard;
        scoreBoard.add_entityManager(entityManager);

        Log.d("Protocol", "surfaceCreated before initialization");
        entityManager.initialization();

		Thread t = new Thread(this);
		t.start();
	}

    public EntityManager getEntityManager(){
            return entityManager;
    }

	@Override
	public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
		// TODO Auto-generated method stub
        mBackgroundImage.recycle();
        mBackgroundImage = null;
	}
	
    @Override
    protected void onDraw(Canvas canvas) {
        if (canvas != null) {
            drawGameMap(canvas);
        }
    }

    private void setPlayerStart()
    {
        if (mPlayerUnit == null)
        {
            mGameMap = new GameMap(mGameContext, GameMapPoint);
        }
    }

    private void setupGameMap()
    {
        //Paint for text
        StutasTextPaint = new Paint();
        StutasTextPaint.setStyle(Paint.Style.FILL);
        StutasTextPaint.setColor(Color.BLUE);
        StutasTextPaint.setAntiAlias(true);
        StutasTextPaint.setTextSize(34.0f);
    }

    private void drawGameMap(Canvas canvas)
    {
        if(canvas != null){
            if(mBackgroundImage != null) {
                canvas.drawRect(-5000, -5000, 5000, 5000, background_paint);
                canvas.drawBitmap(mBackgroundImage, GameMapX, GameMapY, null);
            }
            try {
                //Thread.sleep(100);
            }catch(Exception e){}
        }
    }

    // Setup background Night/Day
    public void SetScheme(int scheme) {
        Resources res = mGameContext.getResources();
        switch(scheme){
            case DAY_MODE:
                strScheme = "DAY";

                mBackgroundImage = mBackgroundImage_day;
            break;
            case NIGHT_MODE:
                strScheme = "NIGHT";

                mBackgroundImage = mBackgroundImage_night;
                break;
            default:
                break;
        }
    }

    // Dectect the touch event and manipulate the
    // touched/touch_x/touch_y variables
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		this.touched_x = event.getX();
		this.touched_y = event.getY();
		int action = event.getAction();
		switch(action){
			case MotionEvent.ACTION_DOWN:
                if(onTouchSplitIcon(touched_x,touched_y)){
                    this.touched = false;
                }else if(onTouchScoreBoardIcon(touched_x,touched_y)){
                    this.touched = false;
                } else{
                    this.touched = true;
                }
				break;
			case MotionEvent.ACTION_MOVE:
                if(onTouchSplitIcon(touched_x,touched_y)){
                    this.touched = false;
                }else if(onTouchScoreBoardIcon(touched_x,touched_y)){
                    this.touched = false;
                } else{
                    this.touched = true;
                }
                break;
			case MotionEvent.ACTION_UP:
                this.release_x = event.getX();
                this.release_y = event.getY();
				touched = false;
                if(onTouchSplitIcon(touched_x,touched_y))
                    if((release_x-touched_x<100 || touched_x - release_x <100) && ( touched_y - release_y <100 || release_y - touched_y < 100))
                        this.split_icon_touched = true;
				if(onTouchScoreBoardIcon(touched_x,touched_y))
                    if((release_x-touched_x<100 || touched_x - release_x <100) && ( touched_y - release_y <100 || release_y - touched_y < 100))
                        this.score_icon_touched = true;
                break;
			case MotionEvent.ACTION_CANCEL:
				touched = false;
				break;
			case MotionEvent.ACTION_OUTSIDE:
				touched = false;
				break;
			default:
		}
		return true;
	}

    // Detect if split icon is touched.
    public boolean onTouchSplitIcon(float touched_x,float touched_y){
        boolean fallin_split_y = touched_y < height-CONFIG.ICON_DIS_TO_EDGE
                        && touched_y > height - CONFIG.ICON_DIS_TO_EDGE - CONFIG.ICON_RADIUS*2;
        boolean fallin_split_x = touched_x < width - CONFIG.ICON_DIS_TO_EDGE
                        && touched_x > width - CONFIG.ICON_DIS_TO_EDGE - CONFIG.ICON_RADIUS*3;
        if(fallin_split_y && fallin_split_x){
            return true;
        }else {
            return false;
        }
    }

    // Show the split icon in the down left corner
    public void onDrawSplitIcon(Canvas canvas,int icon_1_color,int icon_2_color) {
        // initialize two circles for the split icon
        split_icon_1.setAntiAlias(true);
        split_icon_1.setColor(icon_1_color);
        split_icon_2.setAntiAlias(true);
        split_icon_2.setColor(icon_2_color);
        if (canvas != null) {
            canvas.drawCircle(width - CONFIG.ICON_DIS_TO_EDGE - CONFIG.ICON_RADIUS
                    , height - CONFIG.ICON_DIS_TO_EDGE - CONFIG.ICON_RADIUS
                    , CONFIG.ICON_RADIUS, split_icon_1);
            canvas.drawCircle(width - CONFIG.ICON_DIS_TO_EDGE - CONFIG.ICON_RADIUS * 2
                    , height - CONFIG.ICON_DIS_TO_EDGE - CONFIG.ICON_RADIUS
                    , CONFIG.ICON_RADIUS, split_icon_2);
        }
    }


    // Detect if score icon is touched.
    public boolean onTouchScoreBoardIcon(float touched_x,float touched_y){
        boolean fallin_score_y = touched_y > CONFIG.ICON_DIS_TO_EDGE
                && touched_y < CONFIG.ICON_DIS_TO_EDGE + CONFIG.ICON_SCORE_Height;

        boolean fallin_score_x = touched_x > width - CONFIG.ICON_DIS_TO_EDGE - CONFIG.ICON_SCORE_Width
                && touched_x < width - CONFIG.ICON_DIS_TO_EDGE;

        if(fallin_score_y && fallin_score_x){
            return true;
        }else {
            return false;
        }
    }

    private void sendBTMessage(String message) {
        // Check that we're actually connected before trying anything
        if (bluetoothService.getState() != BluetoothService.BT_STATE_CONNECTED) {
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            bluetoothService.write(send);
            send=null;
        }
    }


    public void setSensorX(float sensorX) {
        this.sensorX = sensorX;
    }

    public float getSensorX() {
        return sensorX;
    }

    public void setSensorY(float sensorY) {
        this.sensorY = sensorY;
    }

    public float getSensorY() {
        return sensorY;
    }

    public int getPng_height() {
        return png_height;
    }

    public int getPng_width() {
        return png_width;
    }


    public ScoreBoard getScoreBoard() {
        return scoreBoard;
    }

    public String getStrScheme() {
        return strScheme;
    }

    public void stop(){
        this.running = false;
        GameMapX = 0;
        GameMapY = 0;
    }

}
