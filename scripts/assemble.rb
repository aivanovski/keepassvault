#!/usr/bin/env ruby

DEBUG_APK_PATH = 'app/build/outputs/apk/debug/app-debug.apk'.freeze
RELEASE_APK_PATH = 'app/build/outputs/apk/release/app-release.apk'.freeze
RELEASE_AAB_PATH = 'app/build/outputs/bundle/release/app-release.aab'.freeze

def project_directory?
  `pwd | awk -F'/' '{print $NF}'`.strip == 'kpassnotes'
end

def dir_path
  `pwd`.strip
end

def check_conditions
  if ARGV.empty?
    puts 'Please specify output path'
    exit 1
  end

  unless project_directory?
    puts 'Script should be launched from project root'
    exit 1
  end
end

def remove_files(files)
  files.each do |file|
    if File.exist? file
      puts "Removing file: #{file}"
      `rm #{file}`
    end
  end
end

def copy_files(sources, destinations)
  for i in 0..(sources.size - 1) do
    puts "Copying file to: #{destinations[i]}"
    `cp "#{sources[i]}" "#{destinations[i]}"`
  end
end

def exists?(files)
  files.all? { |file| File.exist? file }
end

def get_app_version
  major = `cat app/build.gradle | grep 'def versionMajor' | cut -d= -f2`.strip
  minor = `cat app/build.gradle | grep 'def versionMinor' | cut -d= -f2`.strip
  patch = `cat app/build.gradle | grep 'def versionPatch' | cut -d= -f2`.strip
  "#{major}.#{minor}.#{patch}"
end

def assemble
  puts 'Assembling Gplay Debug...'
  `./gradlew app:assembleDebug`

  puts 'Assembling Gplay Release...'
  `./gradlew app:assembleRelease`

  puts 'Assembling Bundle Gplay Release...'
  `./gradlew app:bundleRelease`
end

def main
  check_conditions

  files = [
    "#{dir_path}/#{DEBUG_APK_PATH}",
    "#{dir_path}/#{RELEASE_APK_PATH}",
    "#{dir_path}/#{RELEASE_AAB_PATH}"
  ]

  remove_files(files)
  assemble

  if exists?(files)
    puts 'Success'
    output_dir = ARGV[0].strip.gsub('~', '$HOME')
    version = get_app_version
    destinations = [
      "#{output_dir}/app-debug-#{version}.apk",
      "#{output_dir}/app-release-#{version}.apk",
      "#{output_dir}/app-release-#{version}.aab"
    ]
    copy_files(files, destinations)
  else
    puts 'Failed'
  end
end

main
