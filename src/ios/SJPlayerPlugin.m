#import "SJPlayerPlugin.h"

@implementation SJPlayerPlugin

-(void)playVideo:(CDVInvokedUrlCommand*)command{
    _command = command;
    //获取command里面的参数
    NSDictionary *rootDic = [command argumentAtIndex:0];
    request = [rootDic objectForKey:@"url"];
    //request = @"rtmp://live.hkstv.hk.lxdns.com/live/hks";
    _isHorizontal = false;
    if(!self.player){
        CGRect screenRect = [[UIScreen mainScreen] bounds];
        CGFloat screenWidth = screenRect.size.width;
        playView = [[UIView alloc]initWithFrame:CGRectMake(0, 60, screenWidth, screenWidth*3/4)];
        [playView setBackgroundColor:[UIColor blackColor]];
        playView.userInteractionEnabled = true;
        UITapGestureRecognizer *tapGesture=[[UITapGestureRecognizer alloc]initWithTarget:self action:@selector(doubleEvent:)];
        tapGesture.numberOfTapsRequired = 2;
        [playView addGestureRecognizer:tapGesture];
        
        [self.viewController.view addSubview:playView];
        [self play];
        _LabelView = [[UILabel alloc] initWithFrame:playView.frame];
        //设置背景色
        _LabelView.backgroundColor = [UIColor clearColor];
        //设置标签文本字体和字体大小
        _LabelView.font = [UIFont fontWithName:@"Arial" size:18.0];
        //设置文本对其方式
        _LabelView.textAlignment = NSTextAlignmentCenter;
        //文本颜色
        _LabelView.textColor = [UIColor orangeColor];
        _LabelView.text = @"加载中...";
        //加入到整个页面中
        [playView addSubview: _LabelView];
    }else{
        [self.player destroy];
        [self play];
        [_LabelView setHidden:NO];
        _LabelView.text = @"加载中";
    }
}

-(void) play{
    //新建播放器
    self.player = [[AliVcMediaPlayer alloc] init];
    //创建播放器，传入显示窗口
    [self.player create:playView];
    self.player.mediaType = MediaType_AUTO;
    //设置超时时间，单位为毫秒
    self.player.timeout = 10000;
    //缓冲区超过设置值时开始丢帧，单位为毫秒。直播时设置，点播设置无效。范围：500～100000
    self.player.dropBufferDuration = 8000;
    //注册通知
    [self addPlayerObserver];

    //传入播放地址，初始化视频，准备播放
    AliVcMovieErrorCode err = [self.player prepareToPlay:[NSURL URLWithString:request]];
    if(err != ALIVC_SUCCESS) {
        return;
    }
    err = [self.player play];
    if(err != ALIVC_SUCCESS) {
        return;
    }
    //开始播放
    [self.player play];
}

- (void)doubleEvent:(UITapGestureRecognizer *)gesture {
    
    if(self.player){
        CGRect screenRect = [[UIScreen mainScreen] bounds];
        if(_isHorizontal){
            playView.frame = CGRectMake(0, 60, screenRect.size.width, screenRect.size.width*3/4);
            [self.player setRenderRotate:0];
        }else{
            playView.frame = CGRectMake(0, 20, screenRect.size.width, screenRect.size.height-20);
            [self.player setRenderRotate:90];
        }
        _isHorizontal = !_isHorizontal;
    }
}

- (void)becomeActive{
    if (self.player && _isPause) {
        [self.player play];
        _isPause = false;
    }
}

- (void)resignActive{
    if (self.player && self.player.isPlaying){
        [self.player pause];
        _isPause = true;
    }
}

- (void)addPlayerObserver {
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(becomeActive)
                                                 name:UIApplicationDidBecomeActiveNotification
                                               object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(resignActive)
                                                 name:UIApplicationWillResignActiveNotification
                                               object:nil];
    
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(OnVideoPrepared:)
                                                 name:AliVcMediaPlayerLoadDidPreparedNotification object:self.player];
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(OnVideoFinish:)
                                                 name:AliVcMediaPlayerPlaybackDidFinishNotification object:self.player];
    
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(OnVideoError:)
                                                 name:AliVcMediaPlayerPlaybackErrorNotification object:self.player];
    
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(OnSeekDone:)
                                                 name:AliVcMediaPlayerSeekingDidFinishNotification object:self.player];
    
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(OnStartCache:)
                                                 name:AliVcMediaPlayerStartCachingNotification object:self.player];
    
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(OnEndCache:)
                                                 name:AliVcMediaPlayerEndCachingNotification object:self.player];
    
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(onVideoStop:)
                                                 name:AliVcMediaPlayerPlaybackStopNotification object:self.player];
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(onVideoFirstFrame:)
                                                 name:AliVcMediaPlayerFirstFrameNotification object:self.player];
}

- (void)onVideoFirstFrame :(NSNotification *)notification{
}

//recieve prepared notification
- (void)OnVideoPrepared:(NSNotification *)notification {
    [_LabelView setHidden:YES];
}

//recieve start cache notification
- (void)OnStartCache:(NSNotification *)notification {
    [_LabelView setHidden:NO];
    _LabelView.text = @"加载中";
}

//recieve end cache notification
- (void)OnEndCache:(NSNotification *)notification {
    [_LabelView setHidden:YES];
}

//recieve error notification
- (void)OnVideoError:(NSNotification *)notification {
    NSDictionary* userInfo = [notification userInfo];
    NSString* errorMsg = [userInfo objectForKey:@"errorMsg"];
    NSNumber* errorCodeNumber = [userInfo objectForKey:@"error"];
    NSLog(@"errorMsg-%@-%@",errorMsg,errorCodeNumber);
    [_LabelView setHidden:NO];
    _LabelView.text = errorMsg;
}

//recieve finish notification
- (void)OnVideoFinish:(NSNotification *)notification {
}

//recieve seek finish notification
- (void)OnSeekDone:(NSNotification *)notification {
}

- (void)onVideoStop:(NSNotification *)notification {
}

-(void)destroy:(CDVInvokedUrlCommand*)command{
    NSLog(@"===destroy start===");
    if (self.viewController.view != NULL) {
        [self.viewController.view willRemoveSubview:playView];
    }
    if (playView != nil) {
        [playView setHidden:YES];
        playView = nil;
    }
    if (self.player) {
        [self.player stop];
        [self.player destroy];
        self.player = nil;
        [self removePlayerObserver];
    }
    NSLog(@"===destroy end===");
}

#pragma mark - remove NSNotification
-(void)removePlayerObserver
{
    [[NSNotificationCenter defaultCenter] removeObserver:self name:UIApplicationDidBecomeActiveNotification object:nil];
    [[NSNotificationCenter defaultCenter ] removeObserver:self name:UIApplicationWillResignActiveNotification object:nil];
    
    [[NSNotificationCenter defaultCenter] removeObserver:self name:AliVcMediaPlayerLoadDidPreparedNotification object:self.player];
    
    [[NSNotificationCenter defaultCenter] removeObserver:self name:AliVcMediaPlayerPlaybackErrorNotification object:self.player];
    
    [[NSNotificationCenter defaultCenter] removeObserver:self name:AliVcMediaPlayerPlaybackDidFinishNotification object:self.player];
    
    [[NSNotificationCenter defaultCenter] removeObserver:self name:AliVcMediaPlayerSeekingDidFinishNotification object:self.player];
    
    [[NSNotificationCenter defaultCenter] removeObserver:self name:AliVcMediaPlayerStartCachingNotification object:self.player];
    
    [[NSNotificationCenter defaultCenter] removeObserver:self name:AliVcMediaPlayerEndCachingNotification object:self.player];
    
    [[NSNotificationCenter defaultCenter] removeObserver:self name:AliVcMediaPlayerPlaybackStopNotification object:self.player];
    [[NSNotificationCenter defaultCenter] removeObserver:self name:AliVcMediaPlayerPlaybackStopNotification object:self.player];
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}

@end
