t = "  "

try:
    import torch

    if torch.cuda.is_available():
        num_devices = torch.cuda.device_count()
        print(f"CUDA: Devices: {num_devices}")
        for i in range(num_devices):
            device_name = torch.cuda.get_device_name(i)
            device_properties = torch.cuda.get_device_properties(i)
            print(f"{t}Device {i}:")
            print(f"{t}{t}Name: {device_name}")
            print(f"{t}{t}CUDA Capability: {device_properties.major}.{device_properties.minor}")
            print(f"{t}{t}Total Memory: {device_properties.total_memory / 1024**3:.2f} GB")
            print(f"{t}{t}Multiprocessors: {device_properties.multi_processor_count}")
    else:
        print("CUDA: not available on this system.")

except ImportError:
    print("CUDA: torch not installed.")