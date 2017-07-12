package fbg.hsbo.de.trackmyfear;

import android.app.Application;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureQueryResult;
import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.portal.Portal;
import com.esri.arcgisruntime.portal.PortalItem;
import com.esri.arcgisruntime.portal.PortalUser;
import com.esri.arcgisruntime.security.AuthenticationManager;
import com.esri.arcgisruntime.security.DefaultAuthenticationChallengeHandler;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.symbology.SimpleRenderer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private MapView mMapView;
    private Portal mPortal;
    private PortalItem mPortalItem;
    private FeatureLayer featureLayer;
    GraphicsOverlay overlay;
    ArcGISMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMapView = (MapView) findViewById(R.id.mapView);
        overlay = new GraphicsOverlay();
        // create simple renderer
        // red diamond point symbol
        SimpleMarkerSymbol pointSymbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.DIAMOND, Color.RED, 10);
        SimpleRenderer pointRenderer = new SimpleRenderer(pointSymbol);
        overlay.setRenderer(pointRenderer);

        //ArcGISMap map = new ArcGISMap(Basemap.Type.OPEN_STREET_MAP, 51.469327, 7.215595, 16);
        //mMapView.setMap(map);
        loadArcGISPortal();



    }

    @Override
    protected void onPause() {
        mMapView.pause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.resume();
    }

    private void loadArcGISPortal() {
        // Set the DefaultAuthenticationChallegeHandler to allow authentication with the portal.
        DefaultAuthenticationChallengeHandler handler = new DefaultAuthenticationChallengeHandler(this);
        AuthenticationManager.setAuthenticationChallengeHandler(handler);
        // Create a Portal object, indicate authentication is required
        mPortal = new Portal("http://www.arcgis.com", true);
        mPortal.addDoneLoadingListener(new Runnable() {
            @Override
            public void run() {
                if (mPortal.getLoadStatus() == LoadStatus.LOADED) {
                    PortalUser user = mPortal.getUser();
                    String userDisplayName = user.getFullName(); // Returns display name of authenticated user
                    String message = userDisplayName + " has succesfully signed in!";
                    Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
                    toast.show();

                    openMap();
                }
            }
        });
        mPortal.loadAsync();
    }

    private void openMap() {
        // get the pre-defined portal id and portal url
        mPortalItem = new PortalItem(mPortal, getResources().getString(R.string.map_item_id));
        // create a map from a PortalItem
        mMap = new ArcGISMap(mPortalItem);

        mMap.addDoneLoadingListener(new Runnable() {
            @Override
            public void run() {
                featureLayer = (FeatureLayer) mMap.getOperationalLayers().get(0);

                mMapView.getGraphicsOverlays().add(overlay);
            }
        });
        // set the map to be displayed in this view
        mMapView.setMap(mMap);
    }

    private void getAreaForPosition() {
        int tolerance = 10;
        double mapTolerance = tolerance * mMapView.getUnitsPerDensityIndependentPixel();
        // create objects required to do a selection with a query
        Envelope envelope = new Envelope(52.1245 - mapTolerance, 7.1584 - mapTolerance, 52.1245 + mapTolerance, 7.1584 + mapTolerance, mMap.getSpatialReference());
        QueryParameters query = new QueryParameters();
        query.setGeometry(envelope);
        // call select features
        final ListenableFuture<FeatureQueryResult> future = featureLayer.selectFeaturesAsync(query, FeatureLayer.SelectionMode.NEW);
        // add done loading listener to fire when the selection returns
        future.addDoneListener(new Runnable() {
            @Override
            public void run() {
                try {
                    //call get on the future to get the result
                    FeatureQueryResult result = future.get();
                    // create an Iterator
                    Iterator<Feature> iterator = result.iterator();
                    Feature feature;
                    // cycle through selections
                    int counter = 0;
                    while (iterator.hasNext()) {
                        feature = iterator.next();
                        counter++;
                        Log.d(getResources().getString(R.string.app_name), "Selection #: " + counter + " Table name: " + feature.getFeatureTable().getTableName());
                    }
                    Toast.makeText(getApplicationContext(), counter + " features selected", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e(getResources().getString(R.string.app_name), "Select feature failed: " + e.getMessage());
                }
            }
        });
    }

    public void clickTrackPosition(View v){
        startGPSTracking();
    }

    private void startGPSTracking(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(getAssets().open("gps.txt")));
                    String line="";
                    while ((line=reader.readLine())!=null){
                        final String gpsText=line;
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(getApplicationContext(), gpsText, Toast.LENGTH_SHORT).show();
                            }
                        });

                        Thread.sleep(5000);
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
