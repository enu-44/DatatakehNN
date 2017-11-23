package com.datatakehnn.activities.fotos;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.datatakehnn.R;
import com.datatakehnn.activities.cables_elemento.adapter.AdapterCablesElemento;
import com.datatakehnn.activities.fotos.adapter.OnItemClickListenerFoto;
import com.datatakehnn.activities.fotos.adapter.RecyclerAdapterFoto;
import com.datatakehnn.activities.menu.MainMenuActivity;
import com.datatakehnn.controllers.ElementoController;
import com.datatakehnn.controllers.FotoController;
import com.datatakehnn.controllers.NovedadController;
import com.datatakehnn.models.element_model.Elemento;
import com.datatakehnn.models.elemento_cable.Elemento_Cable;
import com.datatakehnn.models.foto_model.Foto;
import com.datatakehnn.models.novedad_model.Novedad;
import com.frosquivel.magicalcamera.MagicalCamera;
import com.frosquivel.magicalcamera.MagicalPermissions;
import com.raizlabs.android.dbflow.data.Blob;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import id.zelory.compressor.Compressor;

public class FotosActivity extends AppCompatActivity implements OnItemClickListenerFoto, FotosMainView {

    //UI Elemenets
    @BindView(R.id.container)
    RelativeLayout container;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.ivFoto1)
    ImageView ivFoto1;
    @BindView(R.id.edtDescripcionFoto1)
    EditText edtDescripcionFoto1;
    @BindView(R.id.ivFoto2)
    ImageView ivFoto2;
    @BindView(R.id.edtDescripcionFoto2)
    EditText edtDescripcionFoto2;


    @BindView(R.id.progressBar)
    ProgressBar progressBar;
    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;
    @BindView(R.id.txtResults)
    TextView txtResults;

    //PERMISSION CAMARA
    private MagicalPermissions magicalPermissions;
    private int RESIZE_PHOTO_PIXELS_PERCENTAGE = 50;
    MagicalCamera magicalCamera;

    //Variables globales
    long elemento_id;
    boolean tomarFoto1 = false;
    boolean tomarFoto2 = false;
    boolean tomarFotoNovedad = false;
    boolean yaTomoFoto1 = false;
    boolean yaTomoFoto2 = false;
    String ruta, path;
    Double Latitud, Longitud;
    String latitud, longitud;
    byte[] image;
    Bitmap compressedImage = null;

    //Instancias
    ElementoController elementoController;
    FotoController fotoController;
    NovedadController novedadController;



    //Dataset
    List<Novedad> novedades;
    Novedad novedad = new Novedad();
    public  String Nombre_Novedad;


    //contador de fotos
    int contador = 0;

    //Adapter
    RecyclerAdapterFoto adapter;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fotos);
        ButterKnife.bind(this);

        checkPermissionCamera();
        setToolbarInjection();
        setupInjection();
        initAdapter();
        initRecyclerView();
        loadNovedades();
    }

    //region SETUP INJECTION
    private void setToolbarInjection() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setTitle("Fotos");
    }

    private void setupInjection() {
        this.elementoController = ElementoController.getInstance(this);
        this.fotoController = FotoController.getInstance(this);
        this.novedadController= NovedadController.getInstance(this);
        elemento_id = elementoController.getLast().getElemento_Id();
        novedades = novedadController.getListNovedadesByElementoId(elemento_id);
        Elemento elemento = elementoController.getLast();
        Latitud = elemento.getLatitud();
        Longitud = elemento.getLongitud();
        latitud = String.valueOf(Latitud);
        longitud = String.valueOf(Longitud);
    }

    //endregion

    //region PERMISSION
    private void checkPermissionCamera() {
        String[] permissions = new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        magicalPermissions = new MagicalPermissions(this, permissions);

        /*if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }*/
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Map<String, Boolean> map = magicalPermissions.permissionResult(requestCode, permissions, grantResults);
        for (String permission : map.keySet()) {
            Log.d("PERMISSIONS", permission + " was: " + map.get(permission));
        }
        //Following the example you could also
        //locationPermissions(requestCode, permissions, grantResults);
    }

    //endregion

    //region EVENTS
    @OnClick({R.id.ivFoto1, R.id.ivFoto2})
    public void onViewClicked(View view) {

        boolean cancel_ = false;
        View focusView_ = null;
        switch (view.getId()) {
            case R.id.ivFoto1:
                tomarFoto1 = true;
                tomarFoto2 = false;
                tomarFotoNovedad = false;
                if (TextUtils.isEmpty(edtDescripcionFoto1.getText().toString())) {
                    edtDescripcionFoto1.setError(getString(R.string.error_field_required));
                    focusView_ = edtDescripcionFoto1;
                    cancel_ = true;
                } else {
                    iniciarCamara();
                }
                break;
            case R.id.ivFoto2:
                tomarFoto2 = true;
                tomarFoto1 = false;
                tomarFotoNovedad = false;
                if (TextUtils.isEmpty(edtDescripcionFoto2.getText().toString())) {
                    edtDescripcionFoto2.setError(getString(R.string.error_field_required));
                    focusView_ = edtDescripcionFoto2;
                    cancel_ = true;
                } else {
                    iniciarCamara();
                }
                break;
        }
    }

    //endregion

    //region Methods
    private void iniciarCamara() {
        magicalCamera = new MagicalCamera(this, RESIZE_PHOTO_PIXELS_PERCENTAGE, magicalPermissions);
        //take photo
        try {
            magicalCamera.takePhoto();
        } catch (Exception e) {
            e.getMessage().toString();
        }

        //select picture
        ///magicalCamera.selectedPicture("my_header_name");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            //CALL THIS METHOD EVER
            //magicalCamera.setResizePhoto(100);
            magicalCamera.resultPhoto(requestCode, resultCode, data);
            Bitmap bitmap = magicalCamera.getPhoto();
            //this is for rotate picture in this method
            //magicalCamera.resultPhoto(requestCode, resultCode, data, MagicalCamera.ORIENTATION_ROTATE_180);
            //with this form you obtain the bitmap (in this example set this bitmap in image view)
            if (tomarFoto1 == true) {
                ivFoto1.setImageBitmap(magicalCamera.getPhoto());
                //if you need save your bitmap in device use this method and return the path if you need this
                //You need to send, the bitmap picture, the photo name, the directory name, the picture type, and autoincrement photo name if           //you need this send true, else you have the posibility or realize your standard name for your pictures.
                ruta = magicalCamera.savePhotoInMemoryDevice(magicalCamera.getPhoto(), "IMG_POSTE_" + "Lat:" + latitud + "_Lng:" + longitud + "_foto1", "DataTakeCamara", MagicalCamera.JPEG, false);
            } else if (tomarFoto2 == true) {
                ivFoto2.setImageBitmap(magicalCamera.getPhoto());
                //if you need save your bitmap in device use this method and return the path if you need this
                //You need to send, the bitmap picture, the photo name, the directory name, the picture type, and autoincrement photo name if           //you need this send true, else you have the posibility or realize your standard name for your pictures.
                ruta = magicalCamera.savePhotoInMemoryDevice(magicalCamera.getPhoto(), "IMG_POSTE_" + "Lat:" + latitud + "_Lng:" + longitud + "_foto2", "DataTakeCamara", MagicalCamera.JPEG, false);
            }else if(tomarFotoNovedad== true){
                ruta = magicalCamera.savePhotoInMemoryDevice(magicalCamera.getPhoto(), "IMG_POSTE_" + "Lat:" + latitud + "_Lng:" + longitud + Nombre_Novedad, "DataTakeCamara", MagicalCamera.JPEG, false);
            }




            File file = saveBitmap(bitmap, ruta);

            try {
                compressedImage = new Compressor(this)
                        .setMaxHeight(400)
                        .setQuality(100)
                        .compressToBitmap(file);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (tomarFoto1 == true) {
                path = magicalCamera.savePhotoInMemoryDevice(compressedImage, "IMG_POSTE_" + "Lat:" + latitud + "_Lng:" + longitud + "_foto1", "DataTakeCamara", MagicalCamera.JPEG, false);
                registerFoto1();


            } else if (tomarFoto2 == true) {
                path = magicalCamera.savePhotoInMemoryDevice(compressedImage, "IMG_POSTE_" + "Lat:" + latitud + "_Lng:" + longitud + "_foto2", "DataTakeCamara", MagicalCamera.JPEG, false);
                registerFoto2();
            }else if(tomarFotoNovedad== true){

                path = magicalCamera.savePhotoInMemoryDevice(compressedImage, "IMG_POSTE_" + "Lat:" + latitud + "_Lng:" + longitud + Nombre_Novedad, "DataTakeCamara", MagicalCamera.JPEG, false);
                updateFotoNovedad();
            }




            if (path != null) {
                Toast.makeText(this, "La foto se guardó en la siguiente ruta: " + path, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Sorry your photo dont write in devide", Toast.LENGTH_SHORT).show();
            }

        } else {
            Snackbar.make(container, "No pudo tomar la foto", Snackbar.LENGTH_SHORT).show();
        }

    }

    private void updateFotoNovedad() {
        image = convertBitmapToByte(compressedImage);
        Blob imagenBlob = new Blob(image);
        novedad.setImage_Novedad(imagenBlob);
        novedadController.updateWithFoto(novedad);
        loadNovedades();
    }

    private void registerFoto1() {
        String ruta_foto = path;
        Foto hayFoto = fotoController.getByRutaFotoAndElementoId(ruta_foto, elemento_id);
        if (hayFoto != null) {
            hayFoto.setDescripcion(edtDescripcionFoto1.getText().toString());
            image = convertBitmapToByte(compressedImage);
            Blob imagenBlob = new Blob(image);
            hayFoto.setImage(imagenBlob);
            fotoController.update(hayFoto);
            yaTomoFoto1 = true;
        } else {
            Foto foto = new Foto();
            foto.setElemento_Id(elemento_id);
            foto.setDescripcion(edtDescripcionFoto1.getText().toString());
            image = convertBitmapToByte(compressedImage);
            Blob imagenBlob = new Blob(image);
            foto.setImage(imagenBlob);
            foto.setRuta_Foto(ruta_foto);
            fotoController.register(foto);
            yaTomoFoto1 = true;
        }
    }

    private void registerFoto2() {
        String ruta_foto = path;
        Foto hayFoto = fotoController.getByRutaFotoAndElementoId(ruta_foto, elemento_id);
        if (hayFoto != null) {
            hayFoto.setDescripcion(edtDescripcionFoto2.getText().toString());
            image = convertBitmapToByte(compressedImage);
            Blob imagenBlob = new Blob(image);
            hayFoto.setImage(imagenBlob);
            fotoController.update(hayFoto);
            yaTomoFoto2 = true;
        } else {
            Foto foto = new Foto();
            foto.setElemento_Id(elemento_id);
            foto.setDescripcion(edtDescripcionFoto2.getText().toString());
            image = convertBitmapToByte(compressedImage);
            Blob imagenBlob = new Blob(image);
            foto.setImage(imagenBlob);
            foto.setRuta_Foto(ruta_foto);
            fotoController.register(foto);
            yaTomoFoto2 = true;
        }
    }



    //Convertir bitmap to File
    private File saveBitmap(Bitmap bitmap, String path) {
        File file = null;
        if (bitmap != null) {
            file = new File(path);
        }
        return file;
    }

    private void validarCampos() {
        if (yaTomoFoto1 == false) {
            Snackbar.make(container, "Debe tomar Foto 1", Snackbar.LENGTH_SHORT).show();
        } else if (yaTomoFoto2 == false) {
            Snackbar.make(container, "Debe tomar Foto 2", Snackbar.LENGTH_SHORT).show();
        } else if (contador < novedades.size()) {
            Snackbar.make(container, "Debe registrar todas las fotos de novedades", Snackbar.LENGTH_SHORT).show();
        }else {
            actualizarPoste();
        }
    }

    private void actualizarPoste() {
        Elemento elemento = elementoController.getLast();
        String horaFin = obtenerHora();
        elemento.setHora_Fin(horaFin);
        elementoController.update(elemento);
        //Snackbar.make(container, "Fotos Registradas", Snackbar.LENGTH_SHORT).show();
        final AlertDialog.Builder builder = new AlertDialog.Builder(FotosActivity.this);
        builder.setTitle("Notificación");
        builder.setMessage("¿Confirma todos los datos?");
        builder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                /*try {
                    ((SyncActivity) syncActivity).iniciarServicioUbicacion();
                } catch (Exception e) {
                    e.getMessage().toString();
                }*/
                startActivity(new Intent(getBaseContext(), MainMenuActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
                finish();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private String obtenerHora() {
        //Obtener Hora
        Calendar cal = Calendar.getInstance();
        DateFormat timeFormat = new SimpleDateFormat("HH:mm");
        String hora = timeFormat.format(cal.getTime());
        return hora;
    }

    public byte[] convertBitmapToByte(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        return byteArray;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(true);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }



    private void initAdapter() {
        if (adapter == null) {
            adapter = new RecyclerAdapterFoto(this, new ArrayList<Novedad>(), this);
        }
    }

    private void initRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }


    private void loadNovedades() {
        adapter.clear();
        novedades.clear();
        novedades = novedadController.getListNovedadesByElementoId(elemento_id);
        setContent(novedades);
        resultsList(novedades);
        hideProgress();
    }

    //endregion

    //region MENU
    /*-------------------------------------------------------------------------------------------------------*/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_foto, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_done) {
            validarCampos();

        }
        return super.onOptionsItemSelected(item);
    }



    //endregion

    //region IMPLEMENTS ONITEM CLICK LISTENER
    @Override
    public void onItemClick(Novedad item) {
        tomarFoto2 = false;
        tomarFoto1 = false;
        tomarFotoNovedad = true;
        Nombre_Novedad= item.getDetalle_Tipo_Novedad_Nombre();
        novedad= item;
        if (item.getImage_Novedad() == null) {
            contador = contador + 1;
        }

        iniciarCamara();
    }


    //endregion

    //region IMPLEMENTS METHODS FotosMainView
    @Override
    public void showProgresss() {
        progressBar.setVisibility(View.VISIBLE);

    }

    @Override
    public void hideProgress() {
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onMessageOk(int colorPrimary, String message) {
        int color = Color.WHITE;
        Snackbar snackbar = Snackbar
                .make(findViewById(R.id.container), message, Snackbar.LENGTH_LONG);
        View sbView = snackbar.getView();
        sbView.setBackgroundColor(ContextCompat.getColor(this, colorPrimary));
        TextView textView = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_done, 0, 0, 0);
        // textView.setCompoundDrawablePadding(getResources().getDimensionPixelOffset(R.dimen.activity_horizontal_margin));
        textView.setTextColor(color);
        snackbar.show();
    }

    @Override
    public void onMessageError(int colorPrimary, String message) {
        onMessageOk(colorPrimary, message);
    }

    @Override
    public void resultsList(List<Novedad> listResult) {
        String results = String.format(getString(R.string.results_global_search),
                listResult.size());
        txtResults.setText(results);
    }

    @Override
    public void setContent(List<Novedad> items) {
        adapter.setItems(items);
    }


    //endregion

}
