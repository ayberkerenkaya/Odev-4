import java.util.Random;

/**
 * ============================================================
 *  Final Değerlendirme Ödevi - 1
 *  Client-Side Load Balancer: Softmax Action Selection
 * ============================================================
 *
 *  TEMEL KAVRAMLAR:
 *  • Server (Eylem/Action) : İsteği gönderebileceğimiz K sunucu
 *  • Latency (Gecikme)     : Sunucudan dönen yanıt süresi (ms)
 *  • Reward (Ödül)         : -latency  → gecikme az ise ödül büyük
 *  • Q[k]                  : k. sunucunun tahmini ortalama gecikmesi
 *  • Softmax               : Q değerlerine göre olasılıksal sunucu seçimi
 *  • Temperature (τ)       : Keşif/sömürü dengesi
 *      - τ büyük → tüm sunucular eşit olasılıkla seçilir (keşif)
 *      - τ küçük → en iyi sunucu neredeyse her zaman seçilir (sömürü)
 *
 *  NON-STATIONARY:
 *  Sunucu gecikmeleri zamanla değişir → Sabit ortalama yerine
 *  ağırlıklı güncelleme kullanılır:
 *      Q[k] ← Q[k] + α * (reward - Q[k])
 */
public class Main {

    // ── Sabitler ──────────────────────────────────────────────
    static final int    K         = 4;     // Sunucu sayısı
    static final int    STEPS     = 3000;  // Toplam istek sayısı
    static final double ALPHA     = 0.1;   // Öğrenme hızı
    static final double TAU       = 20.0;  // Softmax temperature (τ)
    static final double DRIFT_STD = 1.0;   // Gecikme kayma miktarı (ms)
    static final double NOISE_STD = 10.0;  // Ölçüm gürültüsü (ms)

    static Random rand = new Random(42);

    // ── Durum Değişkenleri ────────────────────────────────────
    static double[] Q;            // Tahmini Q-değerleri
    static double[] trueLatency;  // Gerçek (gizli) gecikmeler

    public static void main(String[] args) {

        System.out.println("=== Softmax Load Balancer Simülasyonu ===\n");

        // 1) Başlangıç değerlerini ayarla
        initialize();

        // 2) Simülasyonu çalıştır
        double totalReward    = 0;
        int[]  selectionCount = new int[K];

        for (int step = 1; step <= STEPS; step++) {

            // a) Softmax ile sunucu seç
            int server = softmaxSelect();
            selectionCount[server]++;

            // b) Seçilen sunucudan gürültülü gecikme ölç
            double latency = observe(server);

            // c) Ödül = -gecikme (düşük gecikme = yüksek ödül)
            double reward = -latency;
            totalReward += reward;

            // d) Q-değerini ağırlıklı ortalama ile güncelle
            updateQ(server, reward);

            // e) Gerçek gecikmeleri biraz kaydır (non-stationary)
            drift();

            // f) Her 500 adımda ilerlemeyi yazdır
            if (step % 500 == 0) {
                printProgress(step, totalReward, selectionCount);
            }
        }

        // 3) Sonuçları yazdır
        printFinalResults(totalReward, selectionCount);
    }

    // ─────────────────────────────────────────────────────────
    //  BAŞLANGIÇ: Sunuculara rastgele gecikme ata
    // ─────────────────────────────────────────────────────────
    static void initialize() {
        trueLatency = new double[K];
        for (int k = 0; k < K; k++) {
            trueLatency[k] = 50 + rand.nextDouble() * 150; // 50-200 ms
        }

        // Q = 0 → başta hiçbir sunucu hakkında bilgi yok, keşfe açık
        Q = new double[K];

        System.out.println("Başlangıç gerçek gecikmeleri (ms):");
        for (int k = 0; k < K; k++) {
            System.out.printf("  Server-%d: %.1f ms%n", k, trueLatency[k]);
        }
        System.out.println();
    }

    // ─────────────────────────────────────────────────────────
    //  SOFTMAX SEÇİMİ
    //  P(k) = exp(Q[k] / τ) / Σ exp(Q[j] / τ)
    //  Q[k] = -latency olduğundan düşük gecikmeli sunucu daha çok seçilir.
    // ─────────────────────────────────────────────────────────
    static int softmaxSelect() {
        double[] probs = softmaxProbabilities();

        // Roulette wheel seçimi
        double r = rand.nextDouble();
        double cumulative = 0.0;
        for (int k = 0; k < K; k++) {
            cumulative += probs[k];
            if (r <= cumulative) return k;
        }
        return K - 1;
    }

    static double[] softmaxProbabilities() {
        double[] probs = new double[K];

        // Sayısal kararlılık için max değeri çıkar (log-sum-exp trick)
        double maxQ = Double.NEGATIVE_INFINITY;
        for (double q : Q) maxQ = Math.max(maxQ, q);

        double sum = 0.0;
        for (int k = 0; k < K; k++) {
            probs[k] = Math.exp((Q[k] - maxQ) / TAU);
            sum += probs[k];
        }
        for (int k = 0; k < K; k++) probs[k] /= sum;

        return probs;
    }

    // ─────────────────────────────────────────────────────────
    //  GÖZLEM: Gürültülü gecikme ölçümü
    // ─────────────────────────────────────────────────────────
    static double observe(int server) {
        double noise   = rand.nextGaussian() * NOISE_STD;
        double latency = trueLatency[server] + noise;
        return Math.max(1.0, latency); // Negatif gecikme olamaz
    }

    // ─────────────────────────────────────────────────────────
    //  Q GÜNCELLEME: Üstel ağırlıklı ortalama
    //  Q[k] ← Q[k] + α * (reward - Q[k])
    //  Yeni gözlemlere daha fazla, eskilere daha az ağırlık verir.
    // ─────────────────────────────────────────────────────────
    static void updateQ(int server, double reward) {
        Q[server] = Q[server] + ALPHA * (reward - Q[server]);
    }

    // ─────────────────────────────────────────────────────────
    //  DRIFT: Gerçek gecikmeleri zamanla değiştir (non-stationary)
    // ─────────────────────────────────────────────────────────
    static void drift() {
        for (int k = 0; k < K; k++) {
            trueLatency[k] += rand.nextGaussian() * DRIFT_STD;
            trueLatency[k] = Math.max(10, Math.min(500, trueLatency[k]));
        }
    }

    // ─────────────────────────────────────────────────────────
    //  ÇIKTILAR
    // ─────────────────────────────────────────────────────────
    static void printProgress(int step, double totalReward, int[] selectionCount) {
        double avgLatency = -totalReward / step;
        System.out.printf("--- Adım %d | Ortalama Gecikme: %.1f ms ---%n", step, avgLatency);

        double[] probs = softmaxProbabilities();
        for (int k = 0; k < K; k++) {
            System.out.printf("  Server-%d | Gecikme tahmini: %5.1f ms | Olasılık: %4.1f%% | Seçilme: %d%n",
                    k, -Q[k], probs[k] * 100, selectionCount[k]);
        }
        System.out.println();
    }

    static void printFinalResults(double totalReward, int[] selectionCount) {
        System.out.println("==========================================");
        System.out.println("  SONUÇLAR");
        System.out.println("==========================================");
        System.out.printf("  Toplam Adım     : %d%n", STEPS);
        System.out.printf("  Toplam Ödül     : %.0f%n", totalReward);
        System.out.printf("  Ortalama Gecikme: %.2f ms%n", -totalReward / STEPS);
        System.out.println();
        System.out.println("  Sunucu Kullanım Dağılımı:");
        for (int k = 0; k < K; k++) {
            double percent = 100.0 * selectionCount[k] / STEPS;
            System.out.printf("    Server-%d: %d istek (%.1f%%)%n", k, selectionCount[k], percent);
        }
        System.out.println();
        System.out.println("  Son Softmax Olasılıkları:");
        double[] probs = softmaxProbabilities();
        for (int k = 0; k < K; k++) {
            System.out.printf("    Server-%d: %.2f%%%n", k, probs[k] * 100);
        }
    }
}