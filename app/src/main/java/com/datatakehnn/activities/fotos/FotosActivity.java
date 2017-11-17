package com.datatakehnn.activities.fotos;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.datatakehnn.R;
import com.datatakehnn.activities.fotos.adapter.OnItemClickListenerFoto;
import com.datatakehnn.activities.fotos.adapter.RecyclerAdapterFoto;
import com.datatakehnn.activities.menu.MainMenuActivity;
import com.datatakehnn.activities.sync.SyncActivity;
import com.datatakehnn.controllers.ElementoController;
import com.datatakehnn.controllers.FotoController;
import com.datatakehnn.controllers.NovedadController;
import com.datatakehnn.models.element_model.Elemento;
import com.datatakehnn.models.foto_model.Foto;
import com.datatakehnn.models.novedad_model.Novedad;
import com.datatakehnn.services.coords.CoordsService;
import com.raizlabs.android.dbflow.data.Blob;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class FotosActivity extends AppCompatActivity implements OnItemClickListenerFoto {

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.ivFoto1)
    ImageView ivFoto1;
    @BindView(R.id.edtFotoPoste1)
    EditText edtFotoPoste1;
    @BindView(R.id.ivFoto2)
    ImageView ivFoto2;
    @BindView(R.id.edtFotoPoste2)
    EditText edtFotoPoste2;
    @BindView(R.id.container)
    RelativeLayout container;
    @BindView(R.id.ibTomarFoto1)
    ImageButton ibTomarFoto1;
    @BindView(R.id.ibTomarFoto2)
    ImageButton ibTomarFoto2;
    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;


    //Instancias
    NovedadController novedadController;
    ElementoController elementoController;
    FotoController fotoController;


    //Variables globales
    private Uri file;
    long elemento_id;
    boolean foto1 = false, foto2 = false, tomoFoto1 = false, tomoFoto2 = false;
    boolean isEstadoFoto1 = false, isEstadoFoto2 = false, isEstadoFoto1Defecto = false, isEstadoFoto2Defecto = false;
    int contador = 0;

    //Coordenadas
    public Double Latitud;
    public Double Longitud;
    String latitud;
    String longitud;

    //Dataset
    List<Novedad> novedades;
    Novedad novedad = new Novedad();

    //Adapter
    RecyclerAdapterFoto recyclerAdapterFoto;

    //byte[] image
    byte[] image;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fotos);
        ButterKnife.bind(this);
        checkPermissionCamera();
        setToolbarInjection();
        setupInjection();
        getCoordenadas();
        getData();
        initAdapter();
        initRecyclerView();
        //loadRecyclerItems();
    }


    //region SET INJECTION
    private void initAdapter() {
        if (recyclerAdapterFoto == null) {
            recyclerAdapterFoto = new RecyclerAdapterFoto(this, novedades, this);
        }
    }

    private void initRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(recyclerAdapterFoto);
    }

    private void getData() {
        elemento_id = elementoController.getLast().getElemento_Id();
        novedades = novedadController.getListNovedadesByElementoId(elemento_id);
    }

    private void setupInjection() {
        this.novedadController = novedadController.getInstance(this);
        this.elementoController = elementoController.getInstance(this);
        this.fotoController = fotoController.getInstance(this);
    }

    private void setToolbarInjection() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setTitle("Fotos");
    }


    private void getCoordenadas() {
        Elemento elemento = elementoController.getLast();
        Latitud = elemento.getLatitud();
        Longitud = elemento.getLongitud();
        latitud = String.valueOf(Latitud);
        longitud = String.valueOf(Longitud);
    }

    /*
    private void loadRecyclerItems() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.VERTICAL, false);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerAdapterFoto = new RecyclerAdapterFoto(novedades, this);
        recyclerView.setAdapter(recyclerAdapterFoto);
    }*/

    private void checkPermissionCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
    }

    //endregion

    //region MENU
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_foto, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_done) {
            validacionRegisterElement();

        }
        return super.onOptionsItemSelected(item);
    }

    private void validacionRegisterElement() {
        //FOTO POSTE 1
        String fotoPoste1 = edtFotoPoste1.getText().toString();
        boolean cancel = false;
        View focusView = null;

        boolean cancel_2 = false;
        View focusView_2 = null;

        //FOTO POSTE 2
        String fotoPoste2 = edtFotoPoste2.getText().toString();
        boolean cancel_3 = false;
        View focusView_3 = null;


        boolean cancel_4 = false;
        View focusView_4 = null;

        //
        Drawable drawableFoto1 = ivFoto1.getDrawable();
        Drawable drawableFoto2 = ivFoto2.getDrawable();
        Drawable drawableFotoDefault = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            drawableFotoDefault = getDrawable(R.drawable.boton_foto);
        }

        if (TextUtils.isEmpty(fotoPoste1)) {
            edtFotoPoste1.setError(getString(R.string.error_field_required));
            focusView = edtFotoPoste1;
            cancel = true;
        } else if ((tomoFoto1 == false || isEstadoFoto1Defecto == true)) {
            Snackbar.make(container, "Debe Tomar la Foto 1", Snackbar.LENGTH_SHORT).show();
            cancel = true;
        } else if (TextUtils.isEmpty(fotoPoste2)) {
            edtFotoPoste2.setError(getString(R.string.error_field_required));
            focusView = edtFotoPoste2;
            cancel = true;
        } else if ((tomoFoto2 == false || isEstadoFoto2Defecto == true)) {
            Snackbar.make(container, "Debe Tomar la Foto 2", Snackbar.LENGTH_SHORT).show();
            cancel = true;
        } else if (contador < novedades.size()) {
            Snackbar.make(container, "Debe registrar todas las fotos de novedades", Snackbar.LENGTH_SHORT).show();
        } else {
            registerAll();
        }
    }
    //endregion

    //region EVENTS
    @OnClick({R.id.ibTomarFoto1, R.id.ibTomarFoto2, R.id.ivFoto1, R.id.ivFoto2})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.ibTomarFoto1:
                foto1 = true;
                foto2 = false;
                String fotoPoste1 = edtFotoPoste1.getText().toString();
                boolean cancel = false;
                View focusView = null;
                if (TextUtils.isEmpty(fotoPoste1)) {
                    edtFotoPoste1.setError(getString(R.string.error_field_required));
                    focusView = edtFotoPoste1;
                    cancel = true;
                } else {
                    tomarFoto();
                }
                break;

            case R.id.ivFoto1:
                foto1 = true;
                foto2 = false;
                String fotoPoste1_ = edtFotoPoste1.getText().toString();
                boolean cancel_ = false;
                View focusView_ = null;
                if (TextUtils.isEmpty(fotoPoste1_)) {
                    edtFotoPoste1.setError(getString(R.string.error_field_required));
                    focusView_ = edtFotoPoste1;
                    cancel_ = true;
                } else {
                    tomarFoto();
                }
                break;
            case R.id.ibTomarFoto2:
                foto1 = false;
                foto2 = true;
                String fotoPoste2 = edtFotoPoste2.getText().toString();
                boolean cancel2 = false;
                View focusView2 = null;
                if (TextUtils.isEmpty(fotoPoste2)) {
                    edtFotoPoste2.setError(getString(R.string.error_field_required));
                    focusView2 = edtFotoPoste2;
                    cancel2 = true;
                } else {
                    tomarFoto();
                }
                break;
            case R.id.ivFoto2:
                foto1 = false;
                foto2 = true;
                String fotoPoste2_ = edtFotoPoste2.getText().toString();
                boolean cancel2_ = false;
                View focusView2_ = null;
                if (TextUtils.isEmpty(fotoPoste2_)) {
                    edtFotoPoste2.setError(getString(R.string.error_field_required));
                    focusView2_ = edtFotoPoste2;
                    cancel2_ = true;
                } else {
                    tomarFoto();
                }
                break;
        }
    }
    //endregion

    //region METHODS
    public byte[] convertBitmapToByte(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        return byteArray;
    }

    public byte[] readBytes(Uri uri) throws IOException {
        // this dynamically extends to take the bytes you read
        InputStream inputStream = getContentResolver().openInputStream(uri);
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

        // this is storage overwritten on each iteration with bytes
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        // we need to know how may bytes were read to write them to the byteBuffer
        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }

        // and then we can return your byte array.
        return byteBuffer.toByteArray();
    }

    private void refreshList() {
        recyclerAdapterFoto.clear();
        novedades.clear();
        getData();
        recyclerAdapterFoto.setItems(novedades);
    }


    public void registerAll() {
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
                Intent i = new Intent(getApplicationContext(), MainMenuActivity.class);
                startActivity(i);
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(true);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    //endregion


    //region MÉTODOS DEL ON ITEM CLICK LISTENER

    @Override
    public void onItemClick(Novedad item) {
        novedad = item;
        tomarFotoNovedad();
    }

    //endregion


    //region Tomar Foto Novedades
    private void tomarFotoNovedad() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        file = Uri.fromFile(getOutputMediaFileNovedad());
        intent.putExtra(MediaStore.EXTRA_OUTPUT, file);
        startActivityForResult(intent, 200);
    }

    private File getOutputMediaFileNovedad() {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Datatakeh");

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("Datatakeh", "failed to create directory");
                return null;
            }
        }

        return new File(mediaStorageDir.getPath() + File.separator +
                "IMG_POSTE_" + "Lat:" + latitud + "_Lng:" + longitud + "_NOVEDAD_" +
                novedad.getDetalle_Tipo_Novedad_Nombre() + ".jpg");


    }

    private void tomarFoto() {
        if (foto1 == true) {
            if (isEstadoFoto1 == true) {
                ivFoto1.setImageURI(null);
                ivFoto1.setImageResource(R.drawable.boton_foto);
                isEstadoFoto1Defecto = true;
            }
        } else if (foto2 == true) {
            if (isEstadoFoto2 == true) {
                ivFoto2.setImageURI(null);
                ivFoto2.setImageResource(R.drawable.boton_foto);
                isEstadoFoto2Defecto = true;
            }
        }
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        file = Uri.fromFile(getOutputMediaFile());
        intent.putExtra(MediaStore.EXTRA_OUTPUT, file);
        startActivityForResult(intent, 100);
    }

    private File getOutputMediaFile() {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Datatakeh");

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("Datatakeh", "failed to create directory");
                return null;
            }
        }
        if (foto1 == true) {
            return new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_POSTE_" + "Lat:" + latitud + "_Lng:" + longitud + "_FOTO_1" + ".jpg");
        }
        if (foto2 == true) {
            return new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_POSTE_" + "Lat:" + latitud + "_Lng:" + longitud + "_FOTO_2" + ".jpg");
        } else {
            return null;
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 100) {
            if (resultCode == RESULT_OK) {
                if (foto1 == true) {
                    registerFoto1();
                } else if (foto2 == true) {
                    registerFoto2();
                }
            }
        }
        if (requestCode == 200) {
            if (resultCode == RESULT_OK) {
                try {
                    image = readBytes(file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (novedad.getImage_Novedad() == null) {
                    contador = contador + 1;
                }
                Blob imagenBlob = new Blob(image);
                novedad.setImage_Novedad(imagenBlob);
                novedadController.updateWithFoto(novedad);
                refreshList();
            }
        }

    }


    public void registerFoto1() {
        isEstadoFoto1 = true;
        ivFoto1.setImageURI(file);
        isEstadoFoto1Defecto = false;
        String ruta_foto = file.toString();
        Foto hayFoto = fotoController.getByRutaFotoAndElementoId(ruta_foto, elemento_id);
        if (hayFoto != null) {
            hayFoto.setDescripcion(edtFotoPoste1.getText().toString());
            try {
                image = readBytes(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Blob imagenBlob = new Blob(image);
            hayFoto.setImage(imagenBlob);
            fotoController.update(hayFoto);
            tomoFoto1 = true;
        } else {
            Foto foto = new Foto();
            foto.setElemento_Id(elemento_id);
            foto.setDescripcion(edtFotoPoste1.getText().toString());
            try {
                image = readBytes(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Blob imagenBlob = new Blob(image);
            foto.setImage(imagenBlob);
            foto.setRuta_Foto(file.toString());
            fotoController.register(foto);
            tomoFoto1 = true;
        }

    }

    public void registerFoto2() {
        if (foto2 == true) {
            isEstadoFoto2 = true;
            ivFoto2.setImageURI(file);
            isEstadoFoto2Defecto = false;
            String ruta_foto = file.toString();
            Foto hayFoto = fotoController.getByRutaFotoAndElementoId(ruta_foto, elemento_id);
            if (hayFoto != null) {
                hayFoto.setDescripcion(edtFotoPoste2.getText().toString());
                try {
                    image = readBytes(file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Blob imagenBlob = new Blob(image);
                hayFoto.setImage(imagenBlob);
                fotoController.update(hayFoto);
                tomoFoto2 = true;
            } else {
                Foto foto = new Foto();
                foto.setElemento_Id(elemento_id);
                foto.setDescripcion(edtFotoPoste2.getText().toString());
                try {
                    image = readBytes(file);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Blob imagenBlob = new Blob(image);
                foto.setImage(imagenBlob);
                foto.setRuta_Foto(file.toString());
                fotoController.register(foto);
                tomoFoto2 = true;
            }
        }
    }


    //endregion


}
