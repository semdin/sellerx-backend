# SellerX Backend - Railway Deployment Rehberi

Bu rehber SellerX backend'ini Railway'e PostgreSQL ile birlikte deploy etmek için adım adım talimatlar içerir.

## 🚀 Adım 1: Repository Hazırlığı

```bash
git add .
git commit -m "Ready for Railway deployment"
git push origin main
```

## ☁️ Adım 2: Railway'de Proje Oluşturma

1. **Railway.app**'e gidin
2. **"New Project"** → **"Deploy from GitHub repo"**
3. **`sellerx-backend`** repository'nizi seçin
4. **"Deploy"** tıklayın

## 🗄️ Adım 3: PostgreSQL Database Ekleme & Bağlama

1. Railway dashboard'da **`Ctrl/Cmd + K`** tuşlarına basın
2. **"Database"** → **"Add PostgreSQL"** seçin

### Database'i Backend'e Bağlayın:

3. **Backend service** → **Variables** tab
4. **New Variable** ekleyin:
   ```env
   Name: DATABASE_URL
   Value: jdbc:${{ Postgres.DATABASE_URL }}
   ```
   ⚠️ **Önemli**: `jdbc:` prefix'i ekleyin!
5. **Deploy** (otomatik trigger olur)

## ⚙️ Adım 4: Environment Variables (KRİTİK!)

Backend servisinizde **mutlaka** şu 2 variable'ı ayarlayın:

### JWT Secret Oluşturun:

```powershell
# PowerShell'de çalıştırın:
cd sellerx-backend
.\generate-jwt-secret.ps1
```

### Railway Variables:

```env
SPRING_PROFILES_ACTIVE=production
JWT_SECRET=<generate-jwt-secret.ps1'den çıkan secret>
DATABASE_URL=jdbc:${{ Postgres.DATABASE_URL }}
```

⚠️ **DİKKAT**:

- JWT_SECRET'ı mutlaka yeni generate edin, local'dekini kullanmayın!
- DATABASE_URL'de `jdbc:` prefix'i mutlaka olmalı!

## 🌐 Adım 5: Public URL Oluşturma

1. Backend service → **Settings** → **Networking**
2. **"Generate Domain"** tıklayın

## ✅ Adım 6: Test ve Doğrulama

1. **Deployments** → **View Logs** kontrol edin
2. URL'nizi test edin:
   ```
   GET https://your-app.railway.app/health
   ```

## 🔧 Sorun Giderme

### Deployment Başarısız:

```bash
# Railway CLI ile logs kontrol edin
railway logs
```

### Environment Variables Eksik:

- `SPRING_PROFILES_ACTIVE=production` var mı?
- `JWT_SECRET` set edilmiş mi?

### Database Bağlantı Sorunu:

- PostgreSQL service çalışıyor mu?
- `DATABASE_URL` otomatik oluştu mu?

## 📋 Özet Checklist

- [ ] GitHub'a push yaptım
- [ ] Railway'de proje oluşturdum
- [ ] PostgreSQL ekledim
- [ ] PostgreSQL'i backend'e bağladım (`DATABASE_URL` variable)
- [ ] JWT secret generate ettim
- [ ] Environment variables ayarladım:
  - [ ] `SPRING_PROFILES_ACTIVE=production`
  - [ ] `JWT_SECRET=<yeni-secret>`
  - [ ] `DATABASE_URL=jdbc:${{ Postgres.DATABASE_URL }}`
- [ ] Public domain oluşturdum
- [ ] `/health` endpoint test ettim
- [ ] Flyway migration loglarını kontrol ettim

Bu adımları takip ettikten sonra SellerX backend'iniz Railway'de çalışmaya hazır! 🎉
