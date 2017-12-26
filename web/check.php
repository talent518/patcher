<?php

$versionName = isset($_SERVER['HTTP_VERSION_NAME']) ? $_SERVER['HTTP_VERSION_NAME'] : '3.5';
$lastest = trim(file_get_contents('./lastest'));

if($versionName >= $lastest) {
	renderJson([
		'isNeedUpdate' => false
	]);
} else {
	$newName = $name = $lastest . '.apk';
	$oldName = $versionName . '.apk';
	$isPatch = false;
	if(file_exists($oldName) && (!isset($_SERVER['HTTP_MD5']) || md5File($oldName) === $_SERVER['HTTP_MD5'])) {
		$patchName = $versionName . '-' . $lastest . '.patch';
		file_exists($patchName) or system('bsdiff ' . $oldName . ' ' . $newName . ' ' . $patchName . ' > /dev/null 2>&1');
		if(filesize($patchName) < filesize($name)) {
			$name = $patchName;
			$isPatch = true;
		}
	}
	renderJson([
		'isNeedUpdate' => true,
		'isPatch' => $isPatch,
		'downloadUrl' => $_SERVER['REQUEST_SCHEME'] . '://' . $_SERVER['SERVER_NAME'] . dirname($_SERVER['SCRIPT_NAME']) . '/' . $name,
		'fileSize' => filesize($name),
		'fileName' => $name,
		'md5' => md5File($name),
		'oldMd5' => $isPatch ? md5File($oldName) : null,
		'newMd5' => $isPatch ? md5File($newName) : null
	]);
}

function md5File($file) {
	$hfile = $file . '.md5';
	if(file_exists($hfile)) {
		return file_get_contents($hfile);
	}
	file_put_contents($hfile, $md5 = md5_file($file));
	return $md5;
}

function renderJson($json) {
	@header('Content-Type: application/json; charset=utf-8');
	
	exit(json_encode($json));
}
