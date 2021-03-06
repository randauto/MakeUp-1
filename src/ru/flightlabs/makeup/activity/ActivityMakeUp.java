package ru.flightlabs.makeup.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.IOException;

import ru.flightlabs.commonlib.ErrorInterface;
import ru.flightlabs.commonlib.Settings;
import ru.flightlabs.makeup.ResourcesApp;
import ru.flightlabs.makeup.StateEditor;
import ru.flightlabs.makeup.adapter.AdaptersNotifier;
import ru.flightlabs.makeup.adapter.CategoriesNamePagerAdapter;
import ru.flightlabs.makeup.adapter.CategoriesNewAdapter;
import ru.flightlabs.makeup.adapter.ColorsNewPagerAdapter;
import ru.flightlabs.makeup.adapter.TextNewPagerAdapter;
import ru.flightlabs.makeup.shader.ShaderEffectMakeUp;
import ru.flightlabs.masks.Static;
import ru.flightlabs.masks.camera.FastCameraView;
import ru.flightlabs.masks.camera.FrameCamera;
import ru.flightlabs.masks.model.Utils;
import ru.flightlabs.masks.renderer.MaskRenderer;
import ru.flightlabs.masks.utils.FrameCameraLoad;
import ru.oramalabs.beautykit.BeautyKit;
import ru.oramalabs.beautykit.R;
import us.feras.ecogallery.EcoGallery;
import us.feras.ecogallery.EcoGalleryAdapterView;

/**
 * We should separate view from business logic
 */
public class ActivityMakeUp extends Activity implements AdaptersNotifier, CategoriesNamePagerAdapter.Notification, ErrorInterface {


    long startTime;

    private Tracker mTracker;
    private int currentCategory;

    public static boolean useHsv = false; // false - use colorized
    public static boolean useAlphaCol= true;

    private StateEditor editorEnvironment;
    ResourcesApp resourcesApp;
    ProgressBar progressBar;
    GLSurfaceView gLSurfaceView;
    FastCameraView cameraView;
    MaskRenderer maskRender;
    ImageView rotateCamera;
    ImageView backButton;
    ImageView buttonCamera;
    private PowerManager.WakeLock wakeLock;

    private static final String TAG = "ActivityFast";

    View borderFashion;
    View borderElement;
    View elements;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_makeup);

        Settings.errorClass = this;

        Settings.DIRECTORY_SELFIE = "BeautyKit";

        BeautyKit application = (BeautyKit) getApplication();
        mTracker = application.getDefaultTracker();

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "PreviewWorking");

        cameraView = (FastCameraView) findViewById(R.id.fd_fase_surface_view);

        progressBar = (ProgressBar) findViewById(R.id.progress_bar);

        resourcesApp = new ResourcesApp(this);

        if (Static.LOG_MODE) initDebug();
        rotateCamera = (ImageView)findViewById(R.id.rotate_camera);
        rotateCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mTracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Action")
                        .setAction("RotateCamera")
                        .build());
                cameraView.swapCamera();
            }
        });
        final EcoGallery viewPagerCategories = (EcoGallery) findViewById(R.id.categories);
        final TextNewPagerAdapter pagerCategories = new TextNewPagerAdapter(this, getResources().getStringArray(R.array.categories));
        pagerCategories.selected = StateEditor.FASHION;
        viewPagerCategories.setAdapter(pagerCategories);
        viewPagerCategories.setSelection(StateEditor.FASHION);
        viewPagerCategories.setOnItemSelectedListener(new EcoGalleryAdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(EcoGalleryAdapterView<?> parent, View view, final int position, long id) {
                mTracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Action")
                        .setAction("ChangeCategoryItem")
                        .setLabel(StateEditor.PREFIX_FOR_LABEL[position])
                        .build());
                pagerCategories.selected = position;
                selectedCategory(position);
                //((TextView)view.findViewById(R.id.item_text)).setTextColor(Color.RED);
                // set color
            }

            @Override
            public void onNothingSelected(EcoGalleryAdapterView<?> parent) {

            }
        });

        editorEnvironment = new StateEditor(getApplication().getApplicationContext(), resourcesApp);
        editorEnvironment.init();
        borderFashion = findViewById(R.id.border_fashion);
        borderElement = findViewById(R.id.border_element);
        elements = findViewById(R.id.elements);
        ((SeekBar)findViewById(R.id.opacity)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (Static.LOG_MODE) Log.i(TAG, "opacity " + i);
                editorEnvironment.setOpacity(currentCategory, i);
                gLSurfaceView.requestRender();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        gLSurfaceView = (GLSurfaceView)findViewById(R.id.fd_glsurface);
        gLSurfaceView.setEGLContextClientVersion(2);
        gLSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        gLSurfaceView.getHolder().setFormat(PixelFormat.TRANSPARENT);
        maskRender = new MaskRenderer(this, SplashScreen.compModel, new ShaderEffectMakeUp(this, editorEnvironment));
        gLSurfaceView.setRenderer(maskRender);
        gLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        maskRender.frameCamera = cameraView.frameCamera;

        backButton = (ImageView)findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        buttonCamera = (ImageView)findViewById(R.id.camera_button);
        findViewById(R.id.camera_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!maskRender.staticView) {
                    mTracker.send(new HitBuilders.EventBuilder()
                            .setCategory("Action")
                            .setAction("ToEditPhoto")
                            .build());
                    mTracker.setScreenName("ActivityEdit");
                    mTracker.send(new HitBuilders.ScreenViewBuilder().build());
                    // FIXME should by synchronized, it's fast
                    if (MaskRenderer.poseResult != null && MaskRenderer.poseResult.foundLandmarks != null) {
                        changeToOnlyEditMode();
                    }
                } else {
                    mTracker.send(new HitBuilders.EventBuilder()
                            .setCategory("Action")
                            .setAction("SavePhoto")
                            .build());
                    Static.makePhoto = true;
                    gLSurfaceView.requestRender();
                }
            }
        });
        findViewById(R.id.settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplication(), ActivitySettings.class));
            }
        });

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        float dpHeight = displayMetrics.heightPixels / displayMetrics.density;
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
        if (Static.LOG_MODE) Log.i(TAG, "screen size in dp " + dpWidth + " " + dpHeight);
        // init
        changeCategory(StateEditor.FASHION);
        changeItemInCategory(3);
    }

    @Deprecated
    private void initDebug() {
        findViewById(R.id.checkDebug).setVisibility(View.VISIBLE);
        findViewById(R.id.checkBoxLinear).setVisibility(View.VISIBLE);
        findViewById(R.id.useCalman).setVisibility(View.VISIBLE);
        findViewById(R.id.useCoorized).setVisibility(View.VISIBLE);
        findViewById(R.id.useAlphaColor).setVisibility(View.VISIBLE);
        findViewById(R.id.useFakeCamera).setVisibility(View.VISIBLE);
        ((CheckBox)findViewById(R.id.checkDebug)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Settings.debugMode = b;
            }
        });
        ((CheckBox)findViewById(R.id.checkBoxLinear)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Settings.useLinear = b;
            }
        });
        ((CheckBox)findViewById(R.id.useCalman)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Settings.useKalman = b;
            }
        });
        ((CheckBox)findViewById(R.id.useCoorized)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                useHsv = b;
            }
        });
        ((CheckBox)findViewById(R.id.useAlphaColor)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                useAlphaCol = b;
            }
        });
        ((CheckBox)findViewById(R.id.useFakeCamera)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (Settings.fakeCamera == null) {
                    Settings.fakeCamera = new FrameCamera();
                    try {
                        FrameCameraLoad.loadPic(Settings.fakeCamera, getAssets().open("for_testing/1456.jpg"));
                    } catch (IOException e) {
                        Log.i("FrameCameraLoad", "error " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                Settings.useFakeCamera = b;
            }
        });
        ((CheckBox)findViewById(R.id.useAlphaColor)).setChecked(true);
    }

    @Override
    public void onResume() {
        if (Static.LOG_MODE) Log.i(TAG, "onResume");
        super.onResume();
        mTracker.setScreenName("ActivityMakeUp");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
        wakeLock.acquire();
        gLSurfaceView.onResume();
        Settings.clazz = ActivityPhoto.class;
        // FIXME wrong way
        if (maskRender.staticView) {
            startCameraView();
        }
    }

    private void changeToOnlyEditMode() {
        maskRender.staticView = true;
        gLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        cameraView.disableView();
        buttonCamera.setImageResource(R.drawable.ic_save);
        backButton.setVisibility(View.VISIBLE);
        rotateCamera.setVisibility(View.INVISIBLE);
        gLSurfaceView.requestRender(); // FIXME tis is workaournd
    }

    private void startCameraView() {
        maskRender.staticView = false;
        gLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        buttonCamera.setImageResource(R.drawable.ic_photo);
        backButton.setVisibility(View.GONE);
        rotateCamera.setVisibility(View.VISIBLE);
        cameraView.enableView(); // FIXME not good
    }


    @Override
    protected void onPause() {
        if (Static.LOG_MODE) Log.i(TAG, "onPause");
        super.onPause();
        wakeLock.release();
        gLSurfaceView.onPause();
        //TODO has something todo with FastCameraView (rlease, close etc.)
        cameraView.disableView();
    }

    public void changeCategory(final int position) {
        // FIXME current position not equal current category
        currentCategory = position;
        int resourceId = R.array.colors_shadow;

        boolean fashion = false;
        EcoGallery viewPager = (EcoGallery) findViewById(R.id.elements);
        TypedArray iconsCategory = null;
        if (position == StateEditor.EYE_LASH) {
            iconsCategory = resourcesApp.eyelashesIcons;
            resourceId = R.array.colors_eyelashes;
        } else if (position == StateEditor.EYE_SHADOW) {
            iconsCategory = resourcesApp.eyeshadowIcons;
            resourceId = R.array.colors_shadow;
        } else if (position == StateEditor.EYE_LINE) {
            iconsCategory = resourcesApp.eyelinesIcons;
            resourceId = R.array.colors_eyelashes;
        } else if (position == StateEditor.LIPS) {
            iconsCategory = resourcesApp.lipsSmall;
            resourceId = R.array.colors_lips;
        } else  {
            iconsCategory = resourcesApp.fashionIcons;
            resourceId = R.array.colors_none;
            fashion = true;
        }
        if (fashion) {
            borderFashion.setVisibility(View.VISIBLE);
            borderElement.setVisibility(View.INVISIBLE);
        } else {
            borderFashion.setVisibility(View.INVISIBLE);
            borderElement.setVisibility(View.VISIBLE);
        }
        String[] names = null;
        if (position == StateEditor.FASHION) {
            names = editorEnvironment.getFashionNames();
        }
        final CategoriesNewAdapter pager = new CategoriesNewAdapter(this, iconsCategory, names);
        pager.selected = editorEnvironment.getCurrentIndex(currentCategory);
        viewPager.setAdapter(pager);
        viewPager.setOnItemClickListener(new EcoGalleryAdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(EcoGalleryAdapterView<?> parent, View view, int position, long id) {
                pager.selected = position;
                changeItemInCategory(position);
                //pager.notifyDataSetChanged();
            }
        });
        viewPager.setSelection(editorEnvironment.getCurrentIndex(currentCategory));
        viewPager.setOnItemSelectedListener(new EcoGalleryAdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(EcoGalleryAdapterView<?> parent, View view, int position, long id) {
                pager.selected = position;
                changeItemInCategory(position);
                //pager.notifyDataSetChanged();
            }

            @Override
            public void onNothingSelected(EcoGalleryAdapterView<?> parent) {

            }
        });

        //ViewPager viewPagerColors = (ViewPager) findViewById(R.id.colors);
        if (position == StateEditor.LIPS) {
            //viewPagerColors.setVisibility(View.VISIBLE);
            ColorsNewPagerAdapter pagerColorsNew = new ColorsNewPagerAdapter(this, editorEnvironment.getAllColors(position));
            viewPager.setAdapter(pagerColorsNew);
            pagerColorsNew.selected = editorEnvironment.getColorIndex();
            viewPager.setSelection(editorEnvironment.getColorIndex());
            viewPager.setOnItemSelectedListener(new EcoGalleryAdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(EcoGalleryAdapterView<?> parent, View view, int position2, long id) {
                    //pager.selected = position2;
                    changeColor(editorEnvironment.getAllColors(position)[position2], position2);
                    //pager.notifyDataSetChanged();
                }

                @Override
                public void onNothingSelected(EcoGalleryAdapterView<?> parent) {

                }
            });
            viewPager.setOnItemClickListener(new EcoGalleryAdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(EcoGalleryAdapterView<?> parent, View view, int position2, long id) {
                    changeColor(editorEnvironment.getAllColors(position)[position2], position2);
                    //pager.notifyDataSetChanged();
                }
            });
            //viewPager.setVisibility(View.INVISIBLE);
            //borderFashion.setVisibility(View.INVISIBLE);
            //borderElement.setVisibility(View.INVISIBLE);
        } else {
            //viewPagerColors.setVisibility(View.INVISIBLE);
            viewPager.setVisibility(View.VISIBLE);
        }
        //ColorsPagerAdapter pagerColors = new ColorsPagerAdapter(this, editorEnvironment.getAllColors(position));//getResources().getIntArray(resourceId));
        //viewPagerColors.setAdapter(pagerColors);
        ((SeekBar)findViewById(R.id.opacity)).setProgress(editorEnvironment.getOpacity(currentCategory));
    }

    @Override
    public void onBackPressed() {
        if (maskRender.staticView) {
            mTracker.send(new HitBuilders.EventBuilder()
                    .setCategory("Action")
                    .setAction("FromEditPhoto")
                    .build());
            mTracker.setScreenName("ActivityMakeUp");
            mTracker.send(new HitBuilders.ScreenViewBuilder().build());
            startCameraView();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void changeItemInCategory(int newItem) {
        analytic(currentCategory, newItem);
        if (Static.LOG_MODE) Log.i(TAG, "changeItemInCategory " + newItem);
        editorEnvironment.setCurrentIndexItem(currentCategory, newItem);
        if (currentCategory == StateEditor.FASHION) {
            if (Static.LOG_MODE) Log.i(TAG, "changeItemInCategory ");
            String[] fashions = getResources().getStringArray(R.array.fashion_ic1);
            if (Static.LOG_MODE) Log.i(TAG, "changeItemInCategory " + fashions[newItem]);
            editorEnvironment.setParametersFromFashion(newItem);
        }
        gLSurfaceView.requestRender();
    }

    public void changeColor(int color, int position) {
        analytic(currentCategory, position);
        if (Static.LOG_MODE) Log.i(TAG, "changeColor " + position);
        editorEnvironment.setCurrentColor(currentCategory, position);
        if (maskRender.staticView) {
            gLSurfaceView.requestRender();
        }
    }

    @Override
    public void selectedCategory(int position) {
        changeCategory(position);
    }

    @Override
    public void sendError(UnsatisfiedLinkError e) {
        mTracker.send(new HitBuilders.ExceptionBuilder()
                .setDescription(e.getMessage() +  ":" + e.getLocalizedMessage())
                .setFatal(true)
                .build());
    }

    private void analytic(Integer newCategory, Integer newItem) {
        long curTime = System.currentTimeMillis();
        if ((curTime - startTime) > 1000) {
            mTracker.send(new HitBuilders.EventBuilder()
                    .setCategory("Action")
                    .setAction("ChangeItemInCategory")
                    .setLabel(StateEditor.PREFIX_FOR_LABEL[newCategory] + "_" + newItem)
                    .build());
        }
        startTime = curTime;
    }
}
