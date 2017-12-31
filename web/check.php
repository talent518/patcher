<?php

$versionName = isset($_SERVER['HTTP_VERSION_NAME']) ? $_SERVER['HTTP_VERSION_NAME'] : '3.5';
$lastest = trim(file_get_contents('./lastest'));

if(!greaterVersion($versionName, $lastest)) {
	renderJson([
		'isNeedUpdate' => false
	]);
} else {
	$suffix = ((isset($_SERVER['HTTP_DEBUG']) && $_SERVER['HTTP_DEBUG'] === 'true') ? '-debug' : null);
	$newName = $name = $lastest . $suffix . '.apk';
	$oldName = $versionName . $suffix . '.apk';
	$isPatch = false;
	if(file_exists($oldName) && (!isset($_SERVER['HTTP_MD5']) || md5File($oldName) === $_SERVER['HTTP_MD5'])) {
		$patchName = $versionName . '-' . $lastest . $suffix . '.patch';
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

function greaterVersion($old, $new) {
	$olds = explode('.', trim(preg_replace('/[^0-9\.]+/', '', $old), '.'));
	$news = explode('.', trim(preg_replace('/[^0-9\.]+/', '', $new), '.'));
	foreach($news as $i => $new) {
		if(!isset($olds[$i]) || (int)($new) > (int)($olds[$i])) {
			return true;
		}
	}
	return false;
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
