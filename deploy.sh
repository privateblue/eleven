git merge master
sbt fullOptJS::webpack
sleep 60
cp js/target/scala-2.12/scalajs-bundler/main/app-opt-library.js .
cp js/target/scala-2.12/scalajs-bundler/main/app-opt-loader.js .
cp js/target/scala-2.12/scalajs-bundler/main/app-opt.js .
cp js/eleven.js .