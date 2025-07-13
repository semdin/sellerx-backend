# sellerx-backend

## 🏪 Store Selection System

Bu backend'e store selection (mağaza seçme) sistemi eklenmiştir. Bu sistem kullanıcıların birden fazla mağazası olduğunda, hangi mağaza ile çalışmak istediklerini seçmelerini sağlar.

### Yeni Endpoint'ler:

#### 1. Seçili Mağazayı Getir

- **Endpoint:** `GET /users/selected-store`
- **Headers:** `Authorization: Bearer {token}`
- **Response:**
  ```json
  {
    "selectedStoreId": "store-uuid" // veya "" eğer seçili değilse
  }
  ```

#### 2. Mağaza Seç

- **Endpoint:** `POST /users/selected-store`
- **Headers:** `Authorization: Bearer {token}`
- **Body (JSON):**
  ```json
  {
    "storeId": "store-uuid"
  }
  ```
- **Response:**
  ```json
  {
    "success": true
  }
  ```

#### 3. Dashboard İstatistikleri (Örnek)

- **Endpoint:** `GET /dashboard/stats`
- **Headers:** `Authorization: Bearer {token}`
- **Response:**
  ```json
  {
    "storeId": "selected-store-uuid",
    "totalOrders": 150,
    "totalRevenue": 45000.0,
    "pendingOrders": 12,
    "lowStockProducts": 5
  }
  ```

### 🔧 Database Değişiklikleri:

- `users` tablosuna `selected_store_id` kolonu eklendi
- Foreign key constraint ile `stores` tablosuna bağlandı

### 🚀 Özellikler:

- ✅ Kullanıcı ilk mağazasını oluştururken otomatik seçili hale gelir
- ✅ Mağaza sahipliği kontrolü yapılır
- ✅ JWT token ile kimlik doğrulama
- ✅ Tüm store-specific endpoint'ler seçili mağaza ile çalışır

---

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
