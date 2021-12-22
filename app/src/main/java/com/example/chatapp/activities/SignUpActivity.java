package com.example.chatapp.activities;

import static com.example.chatapp.utilities.Constants.KEY_NAME;
import static com.example.chatapp.utilities.Constants.MIN_PASSWORD_LENGTH;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.example.chatapp.databinding.ActivitySignUpBinding;
import com.example.chatapp.utilities.Constants;
import com.example.chatapp.utilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.regex.Pattern;

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignUpBinding binding;
    private PreferenceManager preferenceManager;
    private String encodedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager =     new PreferenceManager(getApplicationContext());
        setListener();
    }

    // Устанавливает прослушивание элеметов
    private void setListener(){
        // Отслеживает нажатие на текст "Sign In" и возвращается на предыдущую Activity
        binding.textSignIn.setOnClickListener(v -> onBackPressed());
        // Отслеживает нажатие на кнопку "Sign Up" и вызывает функцию "signUp"
        binding.buttonSignUp.setOnClickListener(v -> {
            if(isValidSignUpDetails())
                signUp();
        });
        // Отслеживает нажатие на изображение и вызывает функцию "pickImage"
        binding.imageFrame.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });
    }

    // Отображает сообщения на экране, как уведомление
    private void showToast(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    private void signUp(){
        loading(true);
        // Инициализация Cloud Firestore
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        // Добавляет данные в Cloud Firestore
        HashMap<String, Object> user = new HashMap<>();
        user.put(KEY_NAME, binding.inputName.getText().toString());
        user.put(Constants.KEY_EMAIL, binding.inputEmail.getText().toString());
        user.put(Constants.KEY_PASSWORD, binding.inputFirstPassword.getText().toString());
        user.put(Constants.KEY_IMAGE, encodedImage);
        database.collection(Constants.KEY_COLLECTION_USERS)
                .add(user)
                .addOnSuccessListener(documentReference -> {
                    // Если данные добавленны успешно, то:
                    loading(false);
                    //      сохраняет данные пользователя в приложении в контейнер "preferenceManager"
                    preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                    preferenceManager.putString(Constants.KEY_USER_ID, documentReference.getId());
                    preferenceManager.putString(Constants.KEY_NAME, binding.inputName.getText().toString());
                    preferenceManager.putString(Constants.KEY_IMAGE, encodedImage);
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    //      переходит на новое Activity
                    startActivity(intent);
                })
                .addOnFailureListener(Exception -> {
                    // Если данные не добавленны, то:
                    loading(false);
                    //      отображает ошибку на экране
                    showToast(Exception.getMessage());
                });
    }

    // Кодирует изображение в Код-строку
    private String encodeImage(Bitmap bitmap){
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth/bitmap.getWidth();

        // Создает новое растровое изображение, масштабированное по сравнению
        //          с существующим растровым изображением, когда это возможно
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        // Сжимает изображение
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    // Представляет выбор изображения из устройства
    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if(result.getResultCode() == RESULT_OK){
                    if(result.getData() != null){
                        Uri imageUri = result.getData().getData();
                        try{
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            binding.imageProfile.setImageBitmap(bitmap);
                            binding.textImage.setVisibility(View.GONE);
                            encodedImage = encodeImage(bitmap);
                        } catch (FileNotFoundException e){
                            e.printStackTrace();
                        }
                    }
                }
            }
    );


    // Проверяет валидность введённых данных
    private Boolean isValidSignUpDetails(){
        if(encodedImage == null){
            showToast("Select profile image");
            return false;
        }
        else if(binding.inputName.getText().toString().trim().isEmpty()){
            showToast("Enter Name");
            return false;
        }
        else if(binding.inputEmail.getText().toString().trim().isEmpty()){
            showToast("Enter Email");
            return false;
        }
        else if(!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.getText().toString()).matches()){
            showToast("Enter valid Email");
            return false;
        }
        else if(binding.inputFirstPassword.getText().toString().trim().isEmpty()){
            showToast("Enter Password");
            return false;
        }
        else if(binding.inputFirstPassword.getText().toString().length() < Constants.MIN_PASSWORD_LENGTH){
            showToast("The minimum password length is 6 characters");
            return false;
        }
        else if(binding.inputSecondPassword.getText().toString().trim().isEmpty()){
            showToast("Confirm Password");
            return false;
        }
        else if(!binding.inputFirstPassword.getText().toString().equals(
                binding.inputSecondPassword.getText().toString())){
            showToast("Password & Confirm Password must be same");
            return false;
        }
        else {
            return true;
        }
    }

    // Отображает процесс загрузки
    private void loading(Boolean isLoading){
        if(isLoading){
            binding.buttonSignUp.setVisibility(View.INVISIBLE);     // Кнопка НЕ отображается
            binding.progressBar.setVisibility(View.VISIBLE);        // Загрузка отображается
        }
        else {
            binding.progressBar.setVisibility(View.INVISIBLE);      // Загрузка НЕ отображается
            binding.buttonSignUp.setVisibility(View.VISIBLE);       // Кнопка отображается

        }
    }



}