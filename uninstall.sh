#!/bin/bash

#android application package name
APPLICATION_PACKAGE_NAME="com.ivanovsky.passnotes"

#get device ids and device count
adb_devices_output="$(adb devices | grep device)"
arr=$(echo $adb_devices_output | tr " " "\n")
last_str=""
device_count=0
for str in $arr
do
	if [ "$str" == "device" ]; then
		device_ids[$device_count]=$last_str
		let "device_count += 1"
	fi
	last_str=$str;
done


if [ $device_count -eq 0 ]; then		#if device_count = 0
	#device is not found
	echo "No device found"
elif [ $device_count -eq 1 ]; then		#if device_count = 1
	#uninstall application
	echo "1 device found"
	echo "Exec: adb uninstall $APPLICATION_PACKAGE_NAME"
	adb uninstall $APPLICATION_PACKAGE_NAME
elif [ $device_count -gt 1 ]; then		#if device_count > 1
	echo "$device_count device found"
	echo "Choose device:"

	#choose device
	device_ids[$device_count]="All"
	select selected_option in "${device_ids[@]}"; do
		for item in "${device_ids[@]}"; do
			if [[ $item == $selected_option ]]; then
				break 2
			fi
		done
	done

	#uninstall application
	if [ "$selected_option" == "All" ]; then
		#iterate in a loop all device id and remove application for all
		for ((i=0;i<${#device_ids[@]};++i)); do
			device_id=${device_ids[$i]}
			if [ "$device_id" != "All" ]; then
				echo "Exec: adb -s $device_id uninstall $APPLICATION_PACKAGE_NAME"
				adb -s $device_id uninstall $APPLICATION_PACKAGE_NAME
			fi
		done
	else
		#remove application only for selected device
		echo "Exec: adb -s $selected_option uninstall $APPLICATION_PACKAGE_NAME"
		adb -s $selected_option uninstall $APPLICATION_PACKAGE_NAME
	fi
fi
