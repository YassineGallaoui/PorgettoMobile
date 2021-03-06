package mobile.computing.project;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PointF;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.LocationComponentOptions;
import com.mapbox.mapboxsdk.location.OnCameraTrackingChangedListener;
import com.mapbox.mapboxsdk.location.OnLocationClickListener;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.maps.UiSettings;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.mapbox.mapboxsdk.camera.CameraUpdateFactory.newCameraPosition;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOffset;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, OnLocationClickListener, OnCameraTrackingChangedListener, PermissionsListener {

    public static final String BASE_URL = "https://ewserver.di.unimi.it/mobicomp/mostri/";
    public static final String GET_MAP = "getmap.php";
    public static final String GET_IMAGE = "getimage.php";
    private static final String LAYER_MOSTRI = "LAYER_MOSTRI";
    private static final String LAYER_CARAMELLE = "LAYER_CARAMELLE";
    public String immBase64 = "";
    private MapView mapView;
    private MapboxMap mapboxMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private boolean requestingLocationUpdates = true;
    private boolean isInTrackingMode=true;
    int primaVolta = -1;
    public RequestQueue myRequestQueue = null;
    public RequestQueue myRequestQueue2 = null;
    double latU;
    double lonU;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, "pk.eyJ1IjoieWFzc2luZTk3IiwiYSI6ImNrMzVtZWFwMjA5MmEzZHFqdDRiNGExMzIifQ.gemPT-eRrkMTbxLOB_517w");
        setContentView(R.layout.activity_main);
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        //CHIEDO I PERMESSI PER USARE LA POSIZIONE
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    0);
        }

        //BOTTONE PROFILO
        FloatingActionButton profilo=findViewById(R.id.profileButton);
        profilo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent vaiAlProfilo= new Intent(getApplicationContext(), Profilo.class);
                startActivity(vaiAlProfilo);
            }
        });

        //QUESTO GESTISCE IL MOVIMENTO DELLA CAMERA QUANDO ARRIVA UN UPDATE DELLA POSIZIONE
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                    for (Location location : locationResult.getLocations()) {
                        if (mapboxMap.getCameraPosition().zoom>=16) {
                            double temp = Math.pow(10, 4);
                            double latUA = Math.round(location.getLatitude() * temp) / temp; //NUOVA POSIZIONE RILEVATA ARROTONDATA
                            double lonUA = Math.round(location.getLongitude() * temp) / temp;
                            double latUN = Math.round(latU * temp) / temp; //UGUALE ALLA POSIZIONE APPENA CALCOLATA
                            double lonUN = Math.round(lonU * temp) / temp;
                            if (latUA != latUN || lonUA != lonUN) {
                                Log.d("MainActivity", "OK, POSIZIONE CAMBIATA: " + latUA + ", " + lonUA);
                                Log.d("MainActivity", "POSIZIONE PRECEDENTE: " + latUN + ", " + lonUN);
                                mapboxMap.animateCamera(newCameraPosition(
                                        new CameraPosition.Builder()
                                                .target(new LatLng(location.getLatitude(), location.getLongitude()))
                                                .tilt(30)
                                                .build()), 2000);
                                }
                        }
                        latU = location.getLatitude();
                        lonU = location.getLongitude();
                    }
            }
        };


        final FloatingActionButton you = findViewById(R.id.buttonYou);
        you.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(MainActivity.this, new OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location location) {
                                // Got last known location. In some rare situations this can be null.
                                if (location != null) {
                                    latU = location.getLatitude();
                                    lonU = location.getLongitude();
                                    mapboxMap.animateCamera(newCameraPosition(
                                            impostaPosizione(latU, lonU, true)));//true vuole dire che la posizione che setto è quella dell'utente, quindi le impostazioni della camera saranno diverse
                                } else {
                                    Snackbar.make(findViewById(R.id.mapView), "Posizione non rilevata, attivare il GPS", Snackbar.LENGTH_LONG)
                                            .setAction("Action", null).show();
                                }
                            }
                        }).addOnFailureListener(MainActivity.this, new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                                        != PackageManager.PERMISSION_GRANTED) {
                                    ActivityCompat.requestPermissions(MainActivity.this,
                                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                            0);
                                } else Snackbar.make(findViewById(R.id.mapView), "È necessario fornire i permessi di localizzazione", Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                            }
                        });
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        richiestaDatiMappa();
    }

    //RICHIEDE I DATI DELLA MAPPA AL SERVER
    public void richiestaDatiMappa() {
        //CHIEDO AL SERVER QUALI SONO GLI OGGETTI PRESENTI NELLA MAPPA E LI SALVO
        myRequestQueue = Volley.newRequestQueue(this);
        JSONObject jsonBody = new JSONObject();
        try {
            //vado a prendere il mio session_id dalle shared preferences
            SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                    getString(R.string.preference_file_session_id), Context.MODE_PRIVATE);
            String ses_ID = sharedPref.getString(getString(R.string.preference_file_session_id), "");
            //metto il valore della session_id nella stringa della richiesta
            jsonBody.put("session_id", ses_ID);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest getMap_Request = new JsonObjectRequest
                (Request.Method.POST, BASE_URL + GET_MAP, jsonBody, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        OggettiMappa.getInstance().svuota();
                        OggettiMappa.getInstance().populate(response);
                        Log.d("MainActivity", "Ho chiesto i dati della mappa");
                        richiediImgOggetti();
                        mapView.getMapAsync(MainActivity.this);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("MainActivity", "Non sono riuscito a chiedere i dati della mappa, qualcosa è andato storto.");
                    }
                });

        myRequestQueue.add(getMap_Request);
    }

    private void richiediImgOggetti() {
        for(int i=0; i<OggettiMappa.getInstance().getSize(); i++){

            final Oggetto obj = OggettiMappa.getInstance().getOggetto(i);
            final int idOggetto = obj.getId();
            Log.d("Immaginissima","Sono stupenda hahahah l' id è: "+idOggetto);

            //CHIEDO AL SERVER QUALI SONO GLI OGGETTI PRESENTI NELLA MAPPA E LI SALVO
            myRequestQueue2 = Volley.newRequestQueue(this);
            JSONObject jsonBody = new JSONObject();
            try {
                //vado a prendere il mio session_id dalle shared preferences
                SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                        getString(R.string.preference_file_session_id), Context.MODE_PRIVATE);
                String ses_ID = sharedPref.getString(getString(R.string.preference_file_session_id), "");
                //metto il valore della session_id nella stringa della richiesta
                jsonBody.put("session_id", ses_ID);
                jsonBody.put("target_id", idOggetto);

            } catch (JSONException e) {
                e.printStackTrace();
            }

            JsonObjectRequest getImg_Request = new JsonObjectRequest
                    (Request.Method.POST, BASE_URL + GET_IMAGE, jsonBody, new Response.Listener<JSONObject>() {

                        @Override
                        public void onResponse(JSONObject response) {
                            obj.setImg(response);
                            Log.d("Immaginissima","ok, ho impostato l' immagine di id:" +idOggetto);
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.d("Immaginissima", "OPSSS, qualcosa è andato storto.");
                        }
                    });

            myRequestQueue2.add(getImg_Request);

        }

    }

    //IMPOSTO LE ICONE SULLA MAPPA
    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        Log.d("MainActivity", "È stato  richiamato il metodo onMapReady");
        mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {

                        UiSettings uiSettings = mapboxMap.getUiSettings();
                        uiSettings.setCompassEnabled(false);

                        //INSERISCO I DATI DI XP E LP
                        infoUtente();

                        primaVolta++;
                        //VADO A POSIZIONARMI SULLA MAPPA
                        if (primaVolta == 0) {
                            doveSono();
                        }

                        //POSIZIONO LE ICONE SULLA MAPPA
                        mettiIcone(style);

                        //METTO L'ICONA DELLA POSIZIONE ATTUALE DELL'UTENTE
                        if (primaVolta == 0) {
                            enableLocationComponent(style);
                        }

                        //ATTIVO LA RICHIESTA DI UPDATE DELLA POSIZIONE
                        if (requestingLocationUpdates) {
                            startLocationUpdates();
                        }
                    }
                }
        );

        mapboxMap.addOnMapClickListener(new MapboxMap.OnMapClickListener() {
            @Override
            public boolean onMapClick(@NonNull LatLng point) {
                richiestaDatiMappa();
                clickIcona(point);      //SERVE PER GESTIRE IL CLIC SU UNA ICONA
                return true;
            }
        });
    }

    //PRENDO LP E XP DA METTERE NELLA SCHERMATA MainActivity
    public void infoUtente() {
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                getString(R.string.preference_file_session_id), Context.MODE_PRIVATE);
        String sessionId = sharedPref.getString(getString(R.string.preference_file_session_id), "");
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("session_id", sessionId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        JsonObjectRequest getProfile_Request = new JsonObjectRequest("https://ewserver.di.unimi.it/mobicomp/mostri/getprofile.php",
                jsonBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        TextView tvxp = findViewById(R.id.textView7);
                        TextView tvlp = findViewById(R.id.textView8);
                        User u = new User(response);
                        tvxp.setText("  " + u.getXP() + " XP  ");
                        tvlp.setText("  " + u.getLP() + " LP  ");
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("MainActivity","Richiesta di informazioni dell'utente fallita");
                    }
                }
        );
        requestQueue.add(getProfile_Request);
    }

    //TROVO E IMPOSTO POSIZIONE ATTUALE USER
    public void doveSono() {
        //PRENDO L'ULTIMA POSIZIONE NOTA E MI POSIZIONO LÌ
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(MainActivity.this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            Log.d("MainActivity","POSIZIONE TROVATA");
                            latU = location.getLatitude();
                            lonU = location.getLongitude();
                            mapboxMap.animateCamera(newCameraPosition(
                                    impostaPosizione(latU, lonU, true)));//true vuole dire che la posizione che setto è quella dell'utente, quindi le impostazioni della camera saranno diverse
                        } else {
                            mapboxMap.animateCamera(newCameraPosition(
                                    impostaPosizione(45.471113, 9.182237, false)));
                            Snackbar.make(findViewById(R.id.mapView), "Nessuna posizione rilevata, attivare il GPS", Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                        }
                    }
                });
    }

    //POSIZIONO LE ICONE SULLA MAPPA
    public void mettiIcone(Style style) {

        List<Feature> symbolLayerMonsterFeatureList = new ArrayList<>();
        List<Feature> symbolLayerCandyFeatureList = new ArrayList<>();
        ArrayList<Oggetto> oggetti = OggettiMappa.getInstance().getOggettiMappaList();

        for (int j = 0; j < OggettiMappa.getInstance().getSize(); j++) {
            Oggetto obj = oggetti.get(j);
            String tipoObj = obj.getType();
            double lat = obj.getLat();
            double lon = obj.getLon();

            if (tipoObj.equals("MO")) {
                symbolLayerMonsterFeatureList.add(Feature.fromGeometry(Point.fromLngLat(lon, lat)));
            } else {
                symbolLayerCandyFeatureList.add(Feature.fromGeometry(Point.fromLngLat(lon, lat)));
            }
        }

        // Add the SymbolLayer icon image to the map style
        style.addSource(new GeoJsonSource("SOURCEMOSTRI_ID",
                FeatureCollection.fromFeatures(symbolLayerMonsterFeatureList)));

        style.addSource(new GeoJsonSource("SOURCECANDY_ID",
                FeatureCollection.fromFeatures(symbolLayerCandyFeatureList)));

        // Adding a GeoJson source for the SymbolLayer icons
        style.addImage("ICONMOSTRI_ID", BitmapFactory.decodeResource(
                MainActivity.this.getResources(), R.drawable.monster_icon));

        style.addImage("ICONCANDY_ID", BitmapFactory.decodeResource(
                MainActivity.this.getResources(), R.drawable.candy_icon));

        // Adding the actual SymbolLayer to the map style. An offset is added that the bottom of the red
        // marker icon gets fixed to the coordinate, rather than the middle of the icon being fixed to
        // the coordinate point. This is offset is not always needed and is dependent on the image
        // that you use for the SymbolLayer icon.
        style.addLayer(new SymbolLayer(LAYER_CARAMELLE, "SOURCECANDY_ID")
                .withProperties(PropertyFactory.iconImage("ICONCANDY_ID"),
                        iconAllowOverlap(true),
                        iconOffset(new Float[]{0f, -9f})));

        style.addLayer(new SymbolLayer(LAYER_MOSTRI, "SOURCEMOSTRI_ID")
                .withProperties(PropertyFactory.iconImage("ICONMOSTRI_ID"),
                        iconAllowOverlap(true),
                        iconOffset(new Float[]{0f, -9f})));
    }

    //METTO L'ICONA DELLA POSIZIONE ATTUALE DELL'UTENTE
    @SuppressWarnings({"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {

        // Create and customize the LocationComponent's options
        LocationComponentOptions customLocationComponentOptions = LocationComponentOptions.builder(this)
                .layerBelow(LAYER_MOSTRI)
                .accuracyAlpha(.4f)
                .accuracyColor(Color.CYAN)
                .backgroundTintColor(Color.WHITE)
                .bearingTintColor(Color.WHITE)
                .build();

        // Get an instance of the component
        LocationComponent locationComponent = mapboxMap.getLocationComponent();

        LocationComponentActivationOptions locationComponentActivationOptions =
                LocationComponentActivationOptions.builder(this, loadedMapStyle)
                        .locationComponentOptions(customLocationComponentOptions)
                        .useDefaultLocationEngine(true)
                        .build();

        // Activate with options
        locationComponent.activateLocationComponent(locationComponentActivationOptions);

        // Enable to make component visible
        locationComponent.setLocationComponentEnabled(true);

        // Set the component's camera mode
        locationComponent.setCameraMode(CameraMode.TRACKING_GPS);

        // Set the component's render mode
        locationComponent.setRenderMode(RenderMode.COMPASS);

        // Add the location icon click listener
        locationComponent.addOnLocationClickListener(this);

        // Add the camera tracking listener. Fires if the map camera is manually moved.
        locationComponent.addOnCameraTrackingChangedListener(this);
    }

    //QUANDO CLICCO SU UNA ICONA
    public void clickIcona(@NonNull LatLng point) {

        PointF screenPoint = mapboxMap.getProjection().toScreenLocation(point);
        List<Feature> featuresMostri = mapboxMap.queryRenderedFeatures(screenPoint, LAYER_MOSTRI);
        List<Feature> featuresCandy = mapboxMap.queryRenderedFeatures(screenPoint, LAYER_CARAMELLE);

        double latm = -1;
        double lonm = -1;
        double latc = -1;
        double lonc = -1;

        if (!featuresMostri.isEmpty() && featuresCandy.isEmpty()) { //DOVE HO CLICCATO C'È UN MOSTRO
            Feature selectedFeature = featuresMostri.get(0);
            Point position = (Point) selectedFeature.geometry();
            assert position != null;
            latm = position.latitude();
            lonm = position.longitude();
        }
        if (!featuresCandy.isEmpty() && featuresMostri.isEmpty()) { //DOVE HO CLICCATO C'È UNA CARAMELLA
            Feature selectedFeature = featuresCandy.get(0);
            Point position = (Point) selectedFeature.geometry();
            assert position != null;
            latc = position.latitude();
            lonc = position.longitude();
        }
        if (!featuresCandy.isEmpty() && !featuresMostri.isEmpty()) {
            Feature selectedFeature = featuresCandy.get(0);
            Point position1 = (Point) selectedFeature.geometry();
            Feature selectedFeature2 = featuresCandy.get(0);
            Point position2 = (Point) selectedFeature2.geometry();
            assert position1 != null;
            latm = position1.latitude();
            lonm = position1.longitude();
            assert position2 != null;
            latc = position2.latitude();
            lonc = position2.longitude();
        }
        if (featuresCandy.isEmpty() && featuresMostri.isEmpty()) return; //CIOÈ DOVE HO CLICCATO NON C'È NESSUNA ICONA

        //prendo gli oggetti che ci sono sulla mappa
        ArrayList<Oggetto> objs = OggettiMappa.getInstance().getOggettiMappaList();

        //cerco l'id dell'oggetto cliccato perchè serve per la chiamata
        int nOggetto = -1;
        int posizione = -1;
        double temp = Math.pow(10, 4);
        latm = Math.round(latm * temp) / temp;
        lonm = Math.round(lonm * temp) / temp;
        latc = Math.round(latc * temp) / temp;
        lonc = Math.round(lonc * temp) / temp;
        for (int i = 0; i < objs.size(); i++) {
            Oggetto obj = objs.get(i);
            if ((obj.getLat() == latm && obj.getLon() == lonm) || (obj.getLat() == latc && obj.getLon() == lonc)) {
                nOggetto = obj.getId();
                posizione = i;
                break;
            }
        }

        //Se NON trovo nessun oggetto con quell'ID
        if (nOggetto == -1 || objs.get(posizione).getId() != nOggetto) {
            Snackbar.make(findViewById(R.id.mapView), "Sembra che non ci sia niente qui... Non arrenderti, continua a cercare !", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            return;
        }

        if (objs.get(posizione).getId() == nOggetto) {//Se trovo un oggetto con quell'ID
            //FACCIO LA CHIAMATA PER PRENDERE L'IMMAGINE
            mostraOggetto(nOggetto, posizione);
        }

    }

    //PER INIZIARE GI UPDATE DELLA POSIZIONE
    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(500);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
    }

    //GESTIONE DEL PERMESSO DI LOCALIZZAZIONE
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 0) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(findViewById(R.id.mapView), "È necessario fornire i permessi di localizzazione", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            } else doveSono();
        }
    }

    //SETTO LA POSIZIONE DELLA CAMERA
    public CameraPosition impostaPosizione(final double lat, final double lon, final boolean find) {
        if (find) {
            return new CameraPosition.Builder()
                    .target(new LatLng(lat, lon))
                    .zoom(16)
                    .tilt(30)
                    .build();
        } else {
            return new CameraPosition.Builder()
                    .target(new LatLng(lat, lon))
                    .zoom(10)
                    .build();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

/*    //RICHIEDI INFORMAZIONI DI UN OGGETTO SPECIFICO
    public void richiediImgOggetto(final int numeroOggetto, final int posizione) {

        //PRIMA DI FARE LA CHIAMATA CONTROLLO SE NON HO GIÀ L'IMMAGINE CHE MI SERVE
        SharedPreferences sharedPrefImm = getApplicationContext().getSharedPreferences(getString(R.string.imgObj), Context.MODE_PRIVATE);
        String imm = sharedPrefImm.getString(numeroOggetto+"", "");
        if(!imm.equals("")){
            Log.d("RichiestaImmagine","Ho già l'immagine");
            immBase64=imm;
            ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                    connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED)
                    mostraOggetto(numeroOggetto, posizione);
            else Snackbar.make(findViewById(R.id.mapView), "Errore di connessione", Snackbar.LENGTH_LONG).setAction("Action", null).show();;
        } else {
            Log.d("RichiestaImmagine","Non ho l'immagine, la chiedo");
            //prendo il mio session id perchè serve per la chiamata
            SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                    getString(R.string.preference_file_session_id), Context.MODE_PRIVATE);
            String sessionId = sharedPref.getString(getString(R.string.preference_file_session_id), "");

            //sono pronto per fare la chiamata
            myRequestQueue = Volley.newRequestQueue(this);
            JSONObject jsonBody = new JSONObject();
            try {//metto il valore della session_id e del target_id nella stringa della richiesta
                jsonBody.put("session_id", sessionId + "");
                jsonBody.put("target_id", numeroOggetto + "");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            JsonObjectRequest getImage_Request = new JsonObjectRequest
                    (Request.Method.POST, BASE_URL + GET_IMAGE, jsonBody, new Response.Listener<JSONObject>() {

                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                immBase64 = response.getString("img");
                                SharedPreferences.Editor editor = getApplicationContext().getSharedPreferences(getString(R.string.imgObj), Context.MODE_PRIVATE).edit();
                                editor.putString(numeroOggetto+"", immBase64+"");
                                editor.commit();
                                mostraOggetto(numeroOggetto, posizione);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.d("MainActivity", "Non sono riuscito a prendere l'immagine, qualcosa è andato storto");
                            Snackbar.make(findViewById(R.id.mapView), "Errore di connessione", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                        }
                    });

            myRequestQueue.add(getImage_Request);
        }
    }*/

    //MOSTRA LA SCHERMATA DELLE INFORMAZIONI DEL MOSTRO
    public void mostraOggetto(int nOggetto, int posizione) {
        ArrayList<Oggetto> objs = OggettiMappa.getInstance().getOggettiMappaList();
        Oggetto obj = objs.get(posizione);

        Log.d("MainActivity", "Sto creando l'intent");
        Intent apriInfo = new Intent(MainActivity.this, infoOggetto.class);

        apriInfo.putExtra("id", nOggetto + "");
        apriInfo.putExtra("tipo", obj.getType());
        double cifre = Math.pow(10, 4);
        apriInfo.putExtra("lat", Double.toString(Math.round(obj.getLat() * cifre) / cifre));
        apriInfo.putExtra("lon", Double.toString(Math.round(obj.getLon() * cifre) / cifre));
        apriInfo.putExtra("size", obj.getSize());
        apriInfo.putExtra("nome", obj.getName());
        apriInfo.putExtra("img", obj.getImg());
        startActivity(apriInfo);
        Log.d("MainActivity", "Ho aperto l'activity");
    }

    @Override
    public void onCameraTrackingDismissed() {
        isInTrackingMode = false;
    }

    @Override
    public void onCameraTrackingChanged(int currentMode) {
        //METODO RICHIESTO, MA NON CI INTERESSA
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        //nessuna spiegazione... non annoiamo l'utente ;)
    }

    @Override
    public void onPermissionResult(boolean granted) {
        //L'UNICO CASO L'ABBIAMO GIÀ GESTITO SOPRA, IN UN'ALTRA FUNZIONE MOLTO SIMILE
        //QUESTO È ANCORA DA FARE
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLocationComponentClick() {
    }

}

