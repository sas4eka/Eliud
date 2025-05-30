x=$(printf 'in/%04d.txt' $1)
cd tools
echo $x
../a.out < $x > ../out.txt
cargo run -r -q --bin vis $x ../out.txt
