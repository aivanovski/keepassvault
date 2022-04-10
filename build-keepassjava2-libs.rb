#!/usr/bin/env ruby

KEEPASS_JAVA_URL = 'https://github.com/jorabin/KeePassJava2.git'.freeze

PATCH1 = '/keepassjava2-patches/0001-replace-apache-codecs.patch'.freeze
PATCH2 = '/keepassjava2-patches/0002-protected-properties.patch'.freeze
PATCH3 = '/keepassjava2-patches/0003-fix-recycle-bin-detection.patch'.freeze

SUCCESS = 'success'.freeze
FAILURE = 'failure'.freeze

def read_current_path
  current_path = `cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P`.strip

  if current_path.empty?
    puts "Failed to locate project directory, try to 'cd' into project and run script again"
    exit 1
  end

  current_path
end

def is_directory_exist(path)
  `[ -d #{path} ] && echo #{SUCCESS} || echo #{FAILURE}`.strip == SUCCESS
end

def is_file_exist(path)
  `[ -f #{path} ] && echo #{SUCCESS} || echo #{FAILURE}`.strip == SUCCESS
end

def clone_keepass_java_project
  path = "#{read_current_path}/tmp/KeePassJava2"
  already_cloned = is_directory_exist(path)

  if already_cloned == false
    `git clone #{KEEPASS_JAVA_URL} "#{path}"`
  end

  path
end

def checkout_tag(repo_path, tag_name)
  puts "Checkout tag: #{tag_name}"
  branch_name = "#{tag_name}-branch"
  `cd "#{repo_path}"`

  branches = `cd "#{repo_path}" && git branch`.gsub('*', '').split("\n").map(&:strip)

  if !branches.index(branch_name).nil?
    # If branch already exist
    `cd "#{repo_path}" && git checkout --force master && git branch -d #{branch_name}`
  end

  `cd "#{repo_path}" && git checkout "tags/#{tag_name}" -b "#{branch_name}"`
end

def copy_sources(src_path, dst_path)
  puts "copy sources from: #{src_path} to #{dst_path}"
  `[ -d #{dst_path} ] && rm -r "#{dst_path}"`
  `cp -r "#{src_path}" "#{dst_path}"`
end

def apply_patch(patc_file_path)
  puts "Applying patch: #{patc_file_path}"
  `cd "#{read_current_path}"`

  applied = `git apply "#{patc_file_path}" && echo "#{SUCCESS}"`.strip == SUCCESS
  if applied
    puts 'Successfully applied'
  else
    puts "Failed to apply path file: #{patc_file_path}"
    exit 1
  end
end

def build_library(library_module_name, flavor, library_file_path)
  command = "./gradlew :libs:#{library_module_name}:assemble#{flavor}"

  puts "Assembling library: #{library_module_name}"
  puts "   executed command: #{command}"

  `cd "#{read_current_path}"`
  `[ -f #{library_file_path} ] && rm -r "#{library_file_path}"`
  successfully = `#{command} > /dev/null 2>&1 && echo #{SUCCESS} || echo #{FAILURE}`.strip == SUCCESS
  if successfully
    puts "#{library_module_name} is assembled successfully"
  else
    puts "Failed to assemble module: #{library_module_name}"
    exit 1
  end
end

def main
  current_path = read_current_path

  # clone KeePassJava2 project from github
  keepass_java_path = clone_keepass_java_project
  checkout_tag(keepass_java_path, 'KeePassJava2-2.1.4')

  # copy sources to the libs/*
  copy_sources("#{keepass_java_path}/database/src/main/java/", "#{current_path}/libs/KeePassJava2-database/src/main/java")
  copy_sources("#{keepass_java_path}/simple/src/main/java/", "#{current_path}/libs/KeePassJava2-simple/src/main/java")
  copy_sources("#{keepass_java_path}/kdbx/src/main/java/", "#{current_path}/libs/KeePassJava2-kdbx/src/main/java")

  # apply pathces
  apply_patch("#{current_path + PATCH1}")
  apply_patch("#{current_path + PATCH2}")
  apply_patch("#{current_path + PATCH3}")

  # build Release libraries
  database_aar_release = "#{current_path}/libs/KeePassJava2-database/build/outputs/aar/KeePassJava2-database-release.aar"
  simple_aar_release = "#{current_path}/libs/KeePassJava2-simple/build/outputs/aar/KeePassJava2-simple-release.aar"
  kdbx_aar_release = "#{current_path}/libs/KeePassJava2-kdbx/build/outputs/aar/KeePassJava2-kdbx-release.aar"
  build_library('KeePassJava2-database', 'Release', database_aar_release)
  build_library('KeePassJava2-simple', 'Release', simple_aar_release)
  build_library('KeePassJava2-kdbx', 'Release', kdbx_aar_release)

  # build Debug libraries
  database_aar_debug = "#{current_path}/libs/KeePassJava2-database/build/outputs/aar/KeePassJava2-database-debug.aar"
  simple_aar_debug = "#{current_path}/libs/KeePassJava2-simple/build/outputs/aar/KeePassJava2-simple-debug.aar"
  kdbx_aar_debug = "#{current_path}/libs/KeePassJava2-kdbx/build/outputs/aar/KeePassJava2-kdbx-debug.aar"
  build_library('KeePassJava2-database', 'Debug', database_aar_debug)
  build_library('KeePassJava2-simple', 'Debug', simple_aar_debug)
  build_library('KeePassJava2-kdbx', 'Debug', kdbx_aar_debug)

  # copy assembled libraries to main project
  libs_path = "#{current_path}/app/libs"
  `cp -rf "#{database_aar_release}" "#{libs_path}"`
  `cp -rf "#{database_aar_debug}" "#{libs_path}"`

  `cp -rf "#{simple_aar_release}" "#{libs_path}"`
  `cp -rf "#{simple_aar_debug}" "#{libs_path}"`

  `cp -rf "#{kdbx_aar_release}" "#{libs_path}"`
  `cp -rf "#{kdbx_aar_debug}" "#{libs_path}"`

  puts 'SUCCESS'
end

main
