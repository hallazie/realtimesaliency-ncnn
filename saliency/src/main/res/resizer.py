# coding:utf-8

docker run --volume=$(pwd):/workspace -it tnn-convert:latest python3 ./converter.py onnx2tnn /workspace/fastsal.onnx -optimize -v v3.0 -align  -input_file /workspace/in.txt -ref_file /workspace/ref.txt
