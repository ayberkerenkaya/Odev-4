# 🖧 Softmax Load Balancer

> Final Değerlendirme Ödevi - 1  
> Client-Side Load Balancer: Softmax Action Selection

---

## 📌 Proje Hakkında

Bu proje, **K adet sunucudan** oluşan bir kümeye gelen istekleri akıllıca dağıtan bir **istemci taraflı yük dengeleyici** simülasyonudur.

Klasik Round-Robin veya Random algoritmalarının aksine, bu yük dengeleyici **geçmiş performans verisine** dayanarak öğrenir ve zamanla en hızlı sunucuyu daha sık seçmeye başlar.

---

## 🧠 Temel Kavramlar

| Kavram | Açıklama |
|--------|----------|
| **Action (Eylem)** | Her istek için hangi sunucunun seçileceği |
| **Reward (Ödül)** | `-latency` → gecikme düştükçe ödül artar |
| **Q[k]** | k. sunucunun tahmini performans skoru |
| **Softmax** | Q değerlerine göre olasılıksal sunucu seçimi |
| **Temperature (τ)** | Keşif ve sömürü arasındaki denge |
| **Alpha (α)** | Öğrenme hızı |

---

## ⚙️ Algoritmanın Çalışma Mantığı

```
Her istek için:
  1. Softmax ile sunucu seç       → P(k) = e^(Q[k]/τ) / Σ e^(Q[j]/τ)
  2. Gecikmeyi ölç                → gürültülü gözlem
  3. Ödülü hesapla                → reward = -latency
  4. Q değerini güncelle          → Q[k] = Q[k] + α * (reward - Q[k])
  5. Ortamı kaydır (drift)        → non-stationary simülasyonu
```

---

## 🌡️ Temperature (τ) Parametresi

```
τ büyük (örn: 50) → Tüm sunucular benzer olasılıkla seçilir → Daha fazla keşif
τ küçük (örn: 1)  → En iyi sunucu neredeyse hep seçilir    → Daha açgözlü
```

---

## 📁 Proje Yapısı

```
Main.java
├── main()                  → Ana simülasyon döngüsü
├── initialize()            → Sunuculara başlangıç değeri ata
├── softmaxSelect()         → Olasılıksal sunucu seçimi
├── softmaxProbabilities()  → Softmax formülü hesabı
├── observe()               → Gürültülü gecikme ölçümü
├── updateQ()               → Q değerini güncelle (öğrenme)
├── drift()                 → Non-stationary ortam simülasyonu
├── printProgress()         → Ara sonuçları yazdır
└── printFinalResults()     → Nihai sonuçları yazdır
```

---

## 🚀 Nasıl Çalıştırılır

```bash
# Derle
javac Main.java

# Çalıştır
java Main
```

---

## 🔧 Parametreler

```java
static final int    K         = 4;     // Sunucu sayısı
static final int    STEPS     = 3000;  // Toplam istek sayısı
static final double ALPHA     = 0.1;   // Öğrenme hızı
static final double TAU       = 20.0;  // Softmax temperature
static final double DRIFT_STD = 1.0;   // Gecikme kayma miktarı
static final double NOISE_STD = 10.0;  // Ölçüm gürültüsü
```

---

## 📊 Örnek Çıktı

```
=== Softmax Load Balancer Simülasyonu ===

Başlangıç gerçek gecikmeleri (ms):
  Server-0: 127.3 ms
  Server-1: 73.6 ms
  Server-2: 189.4 ms
  Server-3: 95.1 ms

--- Adım 500 | Ortalama Gecikme: 112.4 ms ---
  Server-0 | Gecikme tahmini: 128.1 ms | Olasılık: 18.3% | Seçilme: 121
  Server-1 | Gecikme tahmini:  74.2 ms | Olasılık: 42.7% | Seçilme: 198
  Server-2 | Gecikme tahmini: 190.6 ms | Olasılık:  8.1% | Seçilme: 87
  Server-3 | Gecikme tahmini:  96.3 ms | Olasılık: 30.9% | Seçilme: 94

==========================================
  SONUÇLAR
==========================================
  Ortalama Gecikme: 98.72 ms

  Sunucu Kullanım Dağılımı:
    Server-0:  412 istek (13.7%)
    Server-1: 1538 istek (51.3%)
    Server-2:  287 istek ( 9.6%)
    Server-3:  763 istek (25.4%)
```

---

## 📚 Non-Stationary Ortam Nedir?

Gerçek hayatta sunucu gecikmeleri sabit kalmaz. Öğle saatinde trafik artar, gece düşer. Bu projedeki `drift()` metodu bunu simüle eder — her adımda gerçek gecikmeler küçük miktarda değişir. Bu yüzden **sabit ortalama** yerine **ağırlıklı güncelleme** kullanılır:

```
Q[k] ← Q[k] + α × (reward - Q[k])
```

`α = 0.1` → Yeni gözleme %10, geçmişe %90 ağırlık verilir.

---

## 🔗 İlgili Kavramlar

- Multi-Armed Bandit Problem
- Reinforcement Learning
- Temporal Difference Learning
- Exploration vs Exploitation Tradeoff
