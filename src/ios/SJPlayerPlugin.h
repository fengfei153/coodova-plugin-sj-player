//
//  SJPlayerPlugin.h
//  video
//
//  Created by wuxiaoqing on 16/12/1.
//
//

#import <Foundation/Foundation.h>
#import <Cordova/CDVPlugin.h>
#import <AliyunPlayerSDK/AliyunPlayerSDK.h>
#import "MainViewController.h"

@interface SJPlayerPlugin : CDVPlugin<UITextViewDelegate>{
    CDVInvokedUrlCommand *_command;
    UIColor *backgroundColor;
    //AliVcMediaPlayer *player;
    UIView *playView;
    CDVPluginResult *_pluginResult ;
    NSString *request;
    Boolean _isHorizontal;
    Boolean _isPause;
}

@property (nonatomic, retain) UILabel *LabelView;
@property (nonatomic, strong) AliVcMediaPlayer *player;

-(void)playVideo:(CDVInvokedUrlCommand*)command;
-(void)destroy:(CDVInvokedUrlCommand*)command;
@end
