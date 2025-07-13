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

#### 3. Dashboard İstatistikleri (Detaylı Trendyol Benzeri)

- **Endpoint:** `GET /dashboard/stats`
- **Headers:** `Authorization: Bearer {token}`
- **Response:**
  ```json
  {
    "storeId": "selected-store-uuid",
    "today": {
      "date": "15 Aralık 2024",
      "revenue": 694.06,
      "currency": "TL",
      "orders": 51,
      "units": 54,
      "returns": 6,
      "adCost": 4.31,
      "estimatedPayment": 430.43,
      "grossProfit": 184.66,
      "netProfit": 33.66,
      "details": {
        "grossRevenue": 694.06,
        "grossSalesCount": 54,
        "netRevenue": 694.06,
        "netSalesCount": 51,
        "adExpenses": -4.31,
        "couponDetails": {
          "total": 0.00,
          "adSpending": -0.81,
          "discount": 0.00,
          "coupon": 0.00
        },
        "productCost": -103.68,
        "shippingCost": -25.50,
        "returnCost": -12.00,
        "otherExpenses": -8.00,
        "internationalDetails": {
          "total": 0.00,
          "internationalServiceFee": 0.00,
          "internationalOperationFee": 0.00,
          "termDelayFee": 0.00,
          "platformServiceFee": 0.00,
          "invoiceCounterSalesFee": 0.00,
          "supplyFailureFee": 0.00,
          "azInternationalOperationFee": 0.00,
          "azPlatformServiceFee": 0.00,
          "eCommerceWithholdingExpense": 0.00
        },
        "extraExpenses": -3.50,
        "packagingDetails": {
          "total": 1.00,
          "officeExpense": 0.00,
          "packaging": 1.00,
          "accountingEtc": 2.00
        },
        "trendyolCommissionAmount": -104.11,
        "vatDifference": -15.20,
        "trendyolCommissionRate": 15.0,
        "returnRate": 11.11,
        "netProfit": 33.66,
        "roi": 32.46,
        "profitMargin": 4.85
      }
    },
    "yesterday": { ... },
    "monthToDate": { ... },
    "lastMonth": { ... }
  }
  ```

📊 **Dashboard Özellikleri:**

- ✅ Bugün, dün, aya kadar, geçen ay verileri
- ✅ Her dönem için detaylı maliyet analizi
- ✅ ROI, kar marjı, iade oranı hesaplamaları
- ✅ Trendyol komisyon ve maliyet detayları
- ✅ **Detaylı Kupon Analizi:** Reklam harcaması, indirim, kupon ayrımı
- ✅ **Kapsamlı Yurt Dışı Operasyon Giderleri:** 10+ farklı ücret türü
- ✅ **Ambalaj ve Ekstra Gider Detayları:** Ofis, muhasebe, ambalaj ayrımı
- ✅ **Gerçekçi Mock Data:** Trendyol API benzeri yapı (gelecek entegrasyona hazır)

### 📋 Detaylı Veri Yapısı:

#### 🎫 Kupon Detayları (`couponDetails`):

- `adSpending` - Reklam Harcaması
- `discount` - İndirim
- `coupon` - Kupon

#### 🌍 Yurt Dışı Operasyon (`internationalDetails`):

- `internationalServiceFee` - Uluslararası Hizmet Bedeli
- `termDelayFee` - Termin Gecikme Bedeli
- `platformServiceFee` - Platform Hizmet Bedeli
- `supplyFailureFee` - Tedarik Edememe
- `eCommerceWithholdingExpense` - E-Ticaret Stopaj Gideri
- ve 5+ ek ücret türü...

#### 📦 Ambalaj Detayları (`packagingDetails`):

- `officeExpense` - Ofis Gideri
- `packaging` - Ambalaj
- `accountingEtc` - Muhasebe vb.

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
