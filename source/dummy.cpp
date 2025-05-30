#include <bits/stdc++.h>
#include <sys/time.h>

using namespace std;

double start;
double get_time() {
    timeval tv;
    gettimeofday(&tv, 0);
    return tv.tv_sec + tv.tv_usec * 1e-6;
}
int currtm() {
    return (int)(1000 * (get_time() - start));
}

static unsigned int g_seed = time(0);
int MYRAND_MAX = 0x7FFF + 1;
int myrand() {
    g_seed = (214013 * g_seed + 2531011);
    return (g_seed >> 16) & 0x7FFF;
}
long long large_rand() {
    return 1LL * myrand() * MYRAND_MAX + myrand();
}

long long ops = 0;
long long opt = 0;

const int TL_ALL = 50;

int main() {
    start = get_time();
    g_seed = 1337;

    long long score = 0;
    while (currtm() < TL_ALL) {
        score += myrand() % 10;
    }

    if (ops != 0) cerr << "OPS: " << ops << endl;
    if (opt != 0) cerr << "OPT: " << opt << endl;
    cerr << "Time = " << currtm() << " ms" << endl;
    cerr << "Score = " << score << endl;
    return 0;
}
