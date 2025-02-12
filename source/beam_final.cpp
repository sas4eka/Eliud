#include <bits/stdc++.h>
#include <sys/time.h>

using namespace std;

const int BEAM = 16000;
const int TL_ALL = 0;

int time_measures = 0;
double start;
double last_time = -1;
int skips = 0;
double get_time() {
    if (time_measures > 1e9) {
        skips++;
        if (skips % (time_measures / 16) != 0) {
            return last_time;
        }
    }
    time_measures++;
    timeval tv;
    gettimeofday(&tv, 0);
    last_time = tv.tv_sec + tv.tv_usec * 1e-6;
    return last_time;
}
int currtm() {
    return (int)(1000 * (get_time() - start));
}

long long ops = 0;
const int N = 20;
const int INF = 1e9;

const int n = 20;

const int EMPTY = 0;
const int GOD = 1;
const int DEMON = 2;

int lshift[N*N];
int rshift[N*N];
int ushift[N*N];
int dshift[N*N];

struct State {
    int gods[N];
    int demons[N];
    int from;
    pair<char, int> how;
    int score;

    State() {}

    State(int from, pair<char, int> how)
        : from(from), how(how) {}

    unsigned long long hashcode() {
        unsigned long long h = 0;
        for (int i = 0; i < N; i++) {
            h *= 231;
            h += gods[i] + 1;
        }
        for (int i = 0; i < N; i++) {
            h *= 231;
            h += demons[i] + 1;
        }
        return h;
    }

    void update_score() {
        score = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (demons[i] >> j & 1) {
                    int dx = min(i, n-1-i) + 1;
                    int dy = min(j, n-1-j) + 1;
                    score += min(dx, dy);
                }
            }
        }
    }
};

void init_shift() {
    int old_score, new_score;
    for (int i = 0; i < n; i++) {
        for (int j = 0; j < n; j++) {
            //L
            old_score = min(min(i, n-1-i), min(j, n-1-j));
            new_score = min(min(i, n-1-i), min(j-1, n-1-j+1));
            lshift[i*N+j] = new_score - old_score;
            //R
            old_score = min(min(i, n-1-i), min(j, n-1-j));
            new_score = min(min(i, n-1-i), min(j+1, n-1-j-1));
            rshift[i*N+j] = new_score - old_score;
            //U
            old_score = min(min(i, n-1-i), min(j, n-1-j));
            new_score = min(min(i-1, n-1-i+1), min(j, n-1-j));
            ushift[i*N+j] = new_score - old_score;
            //D
            old_score = min(min(i, n-1-i), min(j, n-1-j));
            new_score = min(min(i+1, n-1-i-1), min(j, n-1-j));
            dshift[i*N+j] = new_score - old_score;
        }
    }
}

const int MSK = (1<<n) - 1;
void apply_move(State & ns, pair<char, int> & how) {
    char dir = how.first;
    int idx = how.second;
    if (dir == 'L') {
        int i = idx;
        ns.gods[i] = ns.gods[i] >> 1;
        ns.demons[i] = ns.demons[i] >> 1;
    } else if (dir == 'R') {
        int i = idx;
        ns.gods[i] = (ns.gods[i] << 1) & MSK;
        ns.demons[i] = (ns.demons[i] << 1) & MSK;
    } else if (dir == 'U') {
        int j = idx;
        int jm = 1 << j;
        for (int i = 1; i < n; i++) {
            if (ns.gods[i] >> j & 1) {
                ns.gods[i-1] |= jm;
            } else {
                ns.gods[i-1] |= jm;
                ns.gods[i-1] ^= jm;
            }
            if (ns.demons[i] >> j & 1) {
                ns.demons[i-1] |= jm;
            } else {
                ns.demons[i-1] |= jm;
                ns.demons[i-1] ^= jm;
            }
        }
        ns.gods[n-1] |= jm;
        ns.gods[n-1] ^= jm;
        ns.demons[n-1] |= jm;
        ns.demons[n-1] ^= jm;
    } else if (dir == 'D') {
        int j = idx;
        int jm = 1 << j;
        for (int i = n-1; i > 0; i--) {
            if (ns.gods[i-1] >> j & 1) {
                ns.gods[i] |= jm;
            } else {
                ns.gods[i] |= jm;
                ns.gods[i] ^= jm;
            }
            if (ns.demons[i-1] >> j & 1) {
                ns.demons[i] |= jm;
            } else {
                ns.demons[i] |= jm;
                ns.demons[i] ^= jm;
            }
        }
        ns.gods[0] |= jm;
        ns.gods[0] ^= jm;
        ns.demons[0] |= jm;
        ns.demons[0] ^= jm;
    }
}

const int SSZ = 4000000;
int sj = 0;
State states[SSZ];
vector<pair<char, int> > beam(const vector<string> & f0) {
    sj = 0;
    pair<char, int> how0 = {'X', -1};
    State s0(-1, how0);
    for (int i = 0; i < n; i++) {
        s0.gods[i] = s0.demons[i] = 0;
        for (int j = 0; j < n; j++) {
            char orig = f0[i][j];
            if (orig == 'o') {
                s0.gods[i] |= 1<<j;
            } else if (orig == 'x') {
                s0.demons[i] |= 1<<j;
            }
        }
    }
    s0.update_score();
    states[sj] = s0;
    sj++;

    priority_queue<pair<int, int> > old_layer;
    priority_queue<pair<int, int> > new_layer;
    unordered_set<unsigned long long> used;
    old_layer.push({0,0});

    vector<pair<char, int> > valid_moves;
    vector<int> got_i(n);
    vector<int> got_j(n);
    vector<int> lscore(n);
    vector<int> rscore(n);
    vector<int> uscore(n);
    vector<int> dscore(n);
    int step = 0;
    vector<int> old_states;
    while (true) {
        step++;
        int step_score = INF;
        used.clear();
        old_states.clear();

        while (!old_layer.empty()) {
            auto p = old_layer.top();
            old_layer.pop();
            int from = p.second;
            old_states.push_back(from);
        }
        reverse(old_states.begin(), old_states.end());

        for (int from : old_states) {
            const State & os = states[from];

            valid_moves.clear();
            for (int i = 0; i < n; i++) {
                got_i[i] = got_j[i] = 0;
                lscore[i] = 0;
                rscore[i] = 0;
                uscore[i] = 0;
                dscore[i] = 0;
            }
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (os.demons[i] >> j & 1) {
                        got_i[i] = got_j[j] = 1;
                        lscore[i] += lshift[i*N+j];
                        rscore[i] += rshift[i*N+j];
                        uscore[j] += ushift[i*N+j];
                        dscore[j] += dshift[i*N+j];
                    }
                }
            }
            for (int i = 0; i < n; i++) {
                if (!got_i[i]) continue;
                if ((os.gods[i] >> 0 & 1) == 0) {
                    valid_moves.push_back({'L', i});
                }
                if ((os.gods[i] >> (n-1) & 1) == 0) {
                    valid_moves.push_back({'R', i});
                }
            }
            for (int j = 0; j < n; j++) {
                if (!got_j[j]) continue;
                if ((os.gods[0] >> j & 1) == 0) {
                    valid_moves.push_back({'U', j});
                }
                if ((os.gods[n-1] >> j & 1) == 0) {
                    valid_moves.push_back({'D', j});
                }
            }
            if (TL_ALL > 0 && rand() % 2) {
                reverse(valid_moves.begin(), valid_moves.end());
            }
            for (auto & how : valid_moves) {
                int shift = 0;
                char dir = how.first;
                int idx = how.second;
                if (dir == 'L') {
                    shift = lscore[idx];
                } else if (dir == 'R') {
                    shift = rscore[idx];
                } else if (dir == 'U') {
                    shift = uscore[idx];
                } else if (dir == 'D') {
                    shift = dscore[idx];
                }

                int new_score = os.score + shift;
                if (new_layer.size() == BEAM && new_score >= new_layer.top().first) {
                    continue;
                }

                State ns(from, how);
                memcpy(ns.gods, os.gods, sizeof(os.gods));
                memcpy(ns.demons, os.demons, sizeof(os.demons));
                ns.score = new_score;
                apply_move(ns, how);

                auto h = ns.hashcode();
                if (used.count(h)) {
                    continue;
                }
                used.insert(h);
                states[sj] = ns;
                new_layer.push({ns.score, sj});
                sj++;
                if (new_layer.size() > BEAM) {
                    new_layer.pop();
                }
                step_score = min(ns.score, step_score);
            }
        }

        if (TL_ALL == 0) cerr << "STEP " << step << " " << step_score << endl;
        swap(old_layer, new_layer);
        if (step_score == 0) {
            break;
        }
    }
    if (TL_ALL == 0) cerr << "STATES: " << sj << endl;

    int best_i = -1;
    int best_score = 1e9;
    while (!old_layer.empty()) {
        auto p = old_layer.top();
        old_layer.pop();
        if (p.first < best_score) {
            best_score = p.first;
            best_i = p.second;
        }
    }

    int chain_i = best_i;
    vector<int> chain;
    while (chain_i != -1) {
        chain.push_back(chain_i);
        chain_i = states[chain_i].from;
    }
    reverse(chain.begin(), chain.end());

    vector<pair<char, int> > moves;
    for (int i = 1; i < (int) chain.size(); i++) {
        const State & ns = states[chain[i]];
        moves.push_back(ns.how);
    }

    return moves;
}

int main() {
    start = get_time();
    srand(time(0));

    init_shift();

    int nn;
    cin >> nn;
    vector<string> grid(n);
    for (int i = 0; i < n; i++) {
        cin >> grid[i];
    }

    vector<pair<char, int> > best_ans = beam(grid);
    int best_score = best_ans.size();
    while (currtm() < TL_ALL) {
        vector<pair<char, int> > ans = beam(grid);
        if (!ans.empty()) {
            int score = ans.size();
            if (score < best_score) {
                cerr << best_score << " -> " << score << " at " << currtm() << " ms" << endl;
                best_score = score;
                best_ans = ans;
            }
        }
    }

    for (auto & p : best_ans) {
        cout << p.first << " " << p.second << endl;
    }

    if (ops != 0) cerr << "OPS: " << ops << endl;
    int raw_score = 8 * N * N - best_ans.size();

    cerr << "Time = " << currtm() << " ms" << endl;
    cerr << "Score = " << raw_score << endl;

    return 0;
}
