
export ANDROID_NDK=/home/dkicinski/Android/Sdk/ndk/21.3.6528147

rm -rf build
cmake -B build -S . \
 -DANDROID=ON \

cd build && make
cd ..
