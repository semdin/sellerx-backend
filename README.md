# sellerx-backend

## API Kullanımı (Authentication & Kullanıcı İşlemleri)

### 1. Kayıt Ol (Register)

- **Endpoint:** `POST /users`
- **Body (JSON):**
  ```json
  {
    "name": "Mehmet",
    "email": "mehmet@example.com",
    "password": "123456"
  }
  ```
- **Açıklama:** Herkese açık. Yeni kullanıcı oluşturur.

### 2. Giriş Yap (Login)

- **Endpoint:** `POST /auth/login`
- **Body (JSON):**
  ```json
  {
    "email": "mehmet@example.com",
    "password": "123456"
  }
  ```
- **Açıklama:** Doğru bilgilerle giriş yapınca response olarak access token döner. Ayrıca refreshToken cookie olarak gelir.

### 3. Kullanıcıları Listele

- **Endpoint:** `GET /users`
- **Header:**
  ```
  Authorization: Bearer <access_token>
  ```
- **Açıklama:** Login sonrası dönen token ile erişilir.

### 4. Kendi Bilgini Görüntüle

- **Endpoint:** `GET /auth/me`
- **Header:**
  ```
  Authorization: Bearer <access_token>
  ```
- **Açıklama:** Giriş yapan kullanıcının bilgilerini döner.

### 5. Token Yenile (Refresh)

- **Endpoint:** `POST /auth/refresh`
- **Açıklama:** Access token süresi biterse, refreshToken cookie'si ile yeni access token alınır.

---

### Postman Collection

Tüm bu işlemleri kolayca test etmek için hazır Postman collection dosyasını kullanabilirsin:

- `SellerX-Postman-Collection.json` dosyasını Postman'a import et.
- Login sonrası access token otomatik olarak diğer isteklerde kullanılır.
